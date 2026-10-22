import { getLang, setLang, t, useLang } from '../i18n';

// Switches between English and Portuguese. The label shows the language you are in; the tooltip names the other one.
export function LanguageToggle() {
  const lang = useLang();
  return (
    <button
      type="button"
      className="theme-toggle lang-toggle"
      onClick={() => setLang(getLang() === 'pt' ? 'en' : 'pt')}
      aria-label={lang === 'pt' ? 'Switch to English' : t('lang.switchTo')}
      title={lang === 'pt' ? 'Switch to English' : t('lang.switchTo')}
    >
      {lang === 'pt' ? 'PT' : 'EN'}
    </button>
  );
}
