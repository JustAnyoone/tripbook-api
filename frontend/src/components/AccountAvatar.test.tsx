import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

const auth = { user: { id: 1, name: 'Rhyan Pilot', email: 'r@x.com', photoUrl: null as string | null }, setUserPhoto: vi.fn() };
vi.mock('../auth/AuthContext', () => ({ useAuth: () => auth }));

import { AccountAvatar } from './AccountAvatar';

describe('AccountAvatar', () => {
  it('shows the initials when the account has no picture and offers only the upload action', async () => {
    auth.user.photoUrl = null;
    render(<AccountAvatar />);
    expect(screen.getByText('RP')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /account picture|foto da conta/i }));
    expect(screen.getByRole('menuitem', { name: /upload picture|enviar foto/i })).toBeInTheDocument();
    expect(screen.queryByRole('menuitem', { name: /remove picture|remover foto/i })).toBeNull();
  });

  it('shows the picture and offers change and remove when the account has one', async () => {
    auth.user.photoUrl = 'https://res.cloudinary.com/demo/user_1.jpg';
    const { container } = render(<AccountAvatar />);
    expect(container.querySelector('img')).toHaveAttribute('src', 'https://res.cloudinary.com/demo/user_1.jpg');
    await userEvent.click(screen.getByRole('button', { name: /account picture|foto da conta/i }));
    expect(screen.getByRole('menuitem', { name: /change picture|trocar foto/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /remove picture|remover foto/i })).toBeInTheDocument();
  });
});
