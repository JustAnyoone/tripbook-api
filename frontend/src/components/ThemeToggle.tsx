import { t } from '../i18n';
import { useTheme } from '../theme/theme';

// Sun in dark mode (go light), moon in light mode (go dark)
export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const toDark = theme === 'light';
  return (
    <button type="button" className="theme-toggle" onClick={toggle} aria-label={toDark ? t('theme.toDark') : t('theme.toLight')}>
      {toDark ? (
        <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
          <path d="M14.5 10.8A6 6 0 0 1 7.2 3.5a6 6 0 1 0 7.3 7.3Z" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
        </svg>
      ) : (
        <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
          <circle cx="9" cy="9" r="3.4" fill="none" stroke="currentColor" strokeWidth="1.6" />
          <path d="M9 1.6v2M9 14.4v2M1.6 9h2M14.4 9h2M3.8 3.8l1.4 1.4M12.8 12.8l1.4 1.4M3.8 14.2l1.4-1.4M12.8 5.2l1.4-1.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
        </svg>
      )}
    </button>
  );
}
