import { t } from '../i18n';

interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
}

export function ErrorBanner({ message, onRetry }: ErrorBannerProps) {
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      {onRetry && (
        <button type="button" className="button button-quiet" onClick={onRetry}>
          {t('common.tryAgain')}
        </button>
      )}
    </div>
  );
}
