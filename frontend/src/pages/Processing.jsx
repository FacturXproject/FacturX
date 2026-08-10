import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';

const steps = [
  'Lecture du PDF',
  'Extraction du XML',
  'Validation Schematron',
  'Génération du rapport',
];

export default function Processing() {
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const action = params.get('action') || 'verifier';

  const [currentStep, setCurrentStep] = useState(0);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const total = 2200;
    const stepDuration = total / steps.length;

    const interval = setInterval(() => {
      setProgress(p => {
        const next = p + 2;
        setCurrentStep(Math.floor((next / 100) * steps.length));
        return next;
      });
    }, total / 50);

    const timeout = setTimeout(() => {
      clearInterval(interval);
      if (action === 'convertir') {
        navigate('/conversion');
      } else {
        navigate('/rapport?type=with-errors');
      }
    }, total);

    return () => { clearInterval(interval); clearTimeout(timeout); };
  }, [action, navigate]);

  const step = steps[Math.min(currentStep, steps.length - 1)];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px' }}>
      <div style={{ width: '100%', maxWidth: '420px', textAlign: 'center' }}>
        <div style={{ marginBottom: '28px' }}>
          <Loader2 size={32} color="#4a9eff" style={{ animation: 'spin 1s linear infinite' }} />
        </div>

        <h2 style={{ margin: '0 0 6px', fontSize: '17px', fontWeight: 600, color: '#1a1a2e' }}>
          Traitement en cours
        </h2>
        <p style={{ margin: '0 0 28px', fontSize: '13px', color: '#6b7280' }}>
          {step}…
        </p>

        {/* Progress bar */}
        <div style={{ background: '#e5e7eb', borderRadius: '100px', height: '5px', overflow: 'hidden', marginBottom: '20px' }}>
          <div style={{
            height: '100%',
            background: '#4a9eff',
            borderRadius: '100px',
            width: `${progress}%`,
            transition: 'width 0.1s linear',
          }} />
        </div>

        {/* Steps */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', textAlign: 'left' }}>
          {steps.map((s, i) => (
            <div key={s} style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px' }}>
              <div style={{
                width: '20px', height: '20px',
                borderRadius: '50%',
                border: `2px solid ${i < currentStep ? '#4a9eff' : i === currentStep ? '#4a9eff' : '#d1d5db'}`,
                background: i < currentStep ? '#4a9eff' : 'transparent',
                flexShrink: 0,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                {i < currentStep && <span style={{ color: '#fff', fontSize: '11px', fontWeight: 700 }}>✓</span>}
              </div>
              <span style={{ color: i <= currentStep ? '#1a1a2e' : '#9ca3af' }}>{s}</span>
            </div>
          ))}
        </div>
      </div>
      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
