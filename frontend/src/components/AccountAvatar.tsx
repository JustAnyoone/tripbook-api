import { useEffect, useRef, useState } from 'react';
import { authApi, messageOf } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { t } from '../i18n';

// First letters of the first and last name, for accounts without a picture
function initialsOf(name: string | undefined): string {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return (first + last).toUpperCase();
}

// Round avatar in the top bar. Clicking it opens a small menu to upload, change or remove the picture.
export function AccountAvatar() {
  const { user, setUserPhoto } = useAuth();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);

  // Close the menu on a click outside it or on Escape
  useEffect(() => {
    if (!open) return;
    const onPointer = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  async function onFileChosen(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setBusy(true);
    setError(null);
    try {
      const result = await authApi.uploadPhoto(file);
      setUserPhoto(result.photoUrl);
      setOpen(false);
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  async function onRemove() {
    setBusy(true);
    setError(null);
    try {
      await authApi.removePhoto();
      setUserPhoto(null);
      setOpen(false);
    } catch (failure) {
      setError(messageOf(failure));
    } finally {
      setBusy(false);
    }
  }

  const photoUrl = user?.photoUrl ?? null;

  return (
    <div className="avatar-menu" ref={rootRef}>
      <button
        type="button"
        className="avatar"
        aria-label={t('account.menu')}
        aria-haspopup="menu"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        {photoUrl ? <img src={photoUrl} alt="" /> : <span aria-hidden="true">{initialsOf(user?.name)}</span>}
      </button>
      {open && (
        <div className="avatar-popover" role="menu">
          <button type="button" role="menuitem" className="avatar-item" onClick={() => fileRef.current?.click()} disabled={busy}>
            {busy ? t('account.photo.uploading') : photoUrl ? t('account.photo.change') : t('account.photo.upload')}
          </button>
          {photoUrl && (
            <button type="button" role="menuitem" className="avatar-item avatar-item-danger" onClick={onRemove} disabled={busy}>
              {t('account.photo.remove')}
            </button>
          )}
          {error && (
            <p className="avatar-error" role="alert">
              {error}
            </p>
          )}
        </div>
      )}
      <input ref={fileRef} type="file" accept="image/*" hidden onChange={onFileChosen} />
    </div>
  );
}
