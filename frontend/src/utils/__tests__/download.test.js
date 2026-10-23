import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { downloadExport } from '../download';
import api from '../../services/api';

vi.mock('../../services/api');

describe('downloadExport', () => {
  let clicked;

  beforeEach(() => {
    vi.clearAllMocks();
    clicked = [];
    URL.createObjectURL = vi.fn(() => 'blob:export');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function click() {
      clicked.push(this.download);
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("demande l'export au backend et garde le nom de fichier envoye", async () => {
    api.get.mockResolvedValue({
      data: new Blob(['%PDF-']),
      headers: { 'content-disposition': 'attachment; filename="documents.pdf"' },
    });

    await downloadExport('80', 'documents', 'pdf');

    expect(api.get).toHaveBeenCalledWith('/organizations/80/export/documents', {
      params: { format: 'pdf' },
      responseType: 'blob',
    });
    expect(clicked).toEqual(['documents.pdf']);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:export');
  });

  it("utilise un nom par defaut sans en-tete Content-Disposition", async () => {
    api.get.mockResolvedValue({ data: new Blob(['a']), headers: {} });

    await downloadExport('80', 'members', 'csv');

    expect(clicked).toEqual(['members.csv']);
  });
});
