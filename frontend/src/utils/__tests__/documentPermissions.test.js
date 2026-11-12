import { describe, it, expect } from 'vitest';
import { canDeleteDocument } from '../documentPermissions';

const doc = (status, ownerId = 7) => ({ status, ownerId });

describe('canDeleteDocument', () => {
  it('lets an admin delete any document', () => {
    expect(canDeleteDocument(doc('PROCESSING', 1), 'ADMIN', 7)).toBe(true);
  });

  it('lets accountants and clients delete their own documents in any final status', () => {
    for (const status of ['UPLOADED', 'VALID', 'INVALID', 'FAILED']) {
      expect(canDeleteDocument(doc(status), 'ACCOUNTANT', 7)).toBe(true);
      expect(canDeleteDocument(doc(status), 'CLIENT', 7)).toBe(true);
    }
  });

  it('refuses while the document is being processed', () => {
    expect(canDeleteDocument(doc('QUEUED'), 'ACCOUNTANT', 7)).toBe(false);
    expect(canDeleteDocument(doc('PROCESSING'), 'CLIENT', 7)).toBe(false);
  });

  it("refuses another member's document", () => {
    expect(canDeleteDocument(doc('UPLOADED', 1), 'ACCOUNTANT', 7)).toBe(false);
  });
});
