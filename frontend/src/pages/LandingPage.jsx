import { Link } from 'react-router-dom';
import {
  ShieldCheck,
  FileCode2,
  RefreshCw,
  ArrowRight,
} from 'lucide-react';
import Footer from '../components/Footer';

export default function Landing() {
  return (
    <div className="min-h-screen bg-[#f8f9fa] flex flex-col">

      {/* Header */}
      <header className="bg-white border-b border-[#e5e7eb]">
        <div className="max-w-[1100px] mx-auto px-6 py-4 flex items-center justify-between">

          <Link
            to="/"
            className="flex items-center gap-2.5 no-underline"
          >
            <ShieldCheck
              size={24}
              color="#1a2744"
              strokeWidth={2.2}
            />

            <div>
              <div className="text-[#111827] font-bold text-[17px] tracking-[-0.3px]">
                Factur-X
              </div>

              <div className="text-[#6b7280] text-[12px]">
                Conformité & Conversion
              </div>
            </div>
          </Link>

          <div className="flex items-center gap-3">
            <Link
              to="/login"
              className="text-[#4a9eff] text-sm font-medium no-underline px-3 py-2"
            >
              Se connecter
            </Link>

            <Link
              to="/register"
              className="bg-[#1a2744] text-white text-sm font-medium no-underline rounded-md px-4 py-2"
            >
              Créer un compte
            </Link>
          </div>

        </div>
      </header>

      {/* Main */}
      <main className="flex-1">

        {/* Hero */}
        <section className="max-w-[900px] mx-auto px-6 pt-20 pb-16 text-center">

          <div className="inline-flex items-center justify-center bg-[#1a2744] rounded-[12px] p-3 mb-6">
            <ShieldCheck
              size={30}
              color="#4a9eff"
              strokeWidth={2}
            />
          </div>

          <h1 className="text-[#1a1a2e] text-[38px] font-bold tracking-[-0.8px] mb-4">
            Factur-X Validator
          </h1>

          <p className="text-[#6b7280] text-[17px] leading-[1.7] max-w-[650px] mx-auto mb-8">
            Validez, contrôlez et convertissez vos factures électroniques
            depuis une seule plateforme.
          </p>

          <div className="flex items-center justify-center gap-3">
            <Link
              to="/login"
              className="inline-flex items-center gap-2 bg-[#1a2744] text-white no-underline rounded-md px-5 py-2.5 text-sm font-medium"
            >
              Se connecter
              <ArrowRight size={16} />
            </Link>

            <Link
              to="/register"
              className="inline-flex items-center bg-white text-[#1a2744] border border-[#d1d5db] no-underline rounded-md px-5 py-2.5 text-sm font-medium"
            >
              Créer un compte
            </Link>
          </div>

        </section>

        {/* Features */}
        <section className="max-w-[1050px] mx-auto px-6 pb-20">

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">

            <div className="bg-white border border-[#e5e7eb] rounded-[10px] p-6">
              <ShieldCheck
                size={22}
                color="#1a2744"
                className="mb-4"
              />

              <h2 className="text-[#1a1a2e] text-base font-semibold mb-2">
                Validation
              </h2>

              <p className="text-[#6b7280] text-[13px] leading-[1.7] m-0">
                Vérifiez la conformité de vos factures électroniques
                et identifiez rapidement les éventuelles erreurs.
              </p>
            </div>

            <div className="bg-white border border-[#e5e7eb] rounded-[10px] p-6">
              <FileCode2
                size={22}
                color="#1a2744"
                className="mb-4"
              />

              <h2 className="text-[#1a1a2e] text-base font-semibold mb-2">
                Lecture XML
              </h2>

              <p className="text-[#6b7280] text-[13px] leading-[1.7] m-0">
                Consultez facilement le contenu XML de vos documents
                et accédez aux informations essentielles.
              </p>
            </div>

            <div className="bg-white border border-[#e5e7eb] rounded-[10px] p-6">
              <RefreshCw
                size={22}
                color="#1a2744"
                className="mb-4"
              />

              <h2 className="text-[#1a1a2e] text-base font-semibold mb-2">
                Conversion
              </h2>

              <p className="text-[#6b7280] text-[13px] leading-[1.7] m-0">
                Convertissez vos documents électroniques dans les
                formats pris en charge par la plateforme.
              </p>
            </div>

          </div>

        </section>

      </main>

      <Footer />

    </div>
  );
}