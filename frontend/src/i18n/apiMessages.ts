// The API answers in English. Messages the app knows are shown in the user's language; anything else
// (a message added later, a validation text with several parts) is shown as the API wrote it.
const PATTERNS: { pattern: RegExp; text: Record<'en' | 'pt', (match: RegExpMatchArray) => string> }[] = [];

function add(en: string | RegExp, pt: string | ((match: RegExpMatchArray) => string)) {
  const pattern = typeof en === 'string' ? new RegExp(`^${en.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`) : en;
  PATTERNS.push({
    pattern,
    text: { en: (match) => match[0], pt: typeof pt === 'string' ? () => pt : pt },
  });
}

add('Invalid email or password', 'E-mail ou senha inválidos');
add('Authentication required', 'É preciso entrar para continuar');
add(/^Email already registered: (.+)$/, (m) => `Este e-mail já está cadastrado: ${m[1]}`);
add(/^(Trip|Passenger|Destination|User) not found: (.+)$/, (m) => {
  const names: Record<string, string> = { Trip: 'Viagem', Passenger: 'Passageiro', Destination: 'Destino', User: 'Usuário' };
  return `${names[m[1]]} não encontrado(a): ${m[2].replace(/^id /, 'id ')}`;
});
add('A passenger with this document number already exists', 'Já existe um passageiro com este número de documento');
add('Another passenger already has this document number', 'Outro passageiro já tem este número de documento');
add(/^Destination already exists: (.+)$/, (m) => `Este destino já existe: ${m[1]}`);
add('Cannot delete destination: it has trips associated with it', 'Não é possível excluir o destino: ele tem viagens associadas');
add('Departure date cannot be in the past', 'A data de ida não pode estar no passado');
add('Return date cannot be in the past', 'A data de volta não pode estar no passado');
add('Return date cannot be before departure date', 'A data de volta não pode ser anterior à data de ida');
add('status must be PENDING, CONFIRMED or CANCELLED', 'a situação deve ser PENDING, CONFIRMED ou CANCELLED');

// Bean Validation messages; several can arrive joined in one string
const FIELDS: [string, string][] = [
  ['name is required', 'o nome é obrigatório'],
  ['document number is required', 'o número do documento é obrigatório'],
  ['document number must have 6 to 20 letters or digits', 'o número do documento deve ter de 6 a 20 letras ou dígitos'],
  ['email is required', 'o e-mail é obrigatório'],
  ['invalid email', 'e-mail inválido'],
  ['birth date must be in the past', 'a data de nascimento deve estar no passado'],
  ['password is required', 'a senha é obrigatória'],
  ['password must have 8 to 72 characters', 'a senha deve ter de 8 a 72 caracteres'],
  ['city is required', 'a cidade é obrigatória'],
  ['country is required', 'o país é obrigatório'],
  ['departureDate is required', 'a data de ida é obrigatória'],
  ['price is required', 'o preço é obrigatório'],
  ['price must be greater than zero', 'o preço deve ser maior que zero'],
  ['status is required', 'a situação é obrigatória'],
  ['passengerId is required', 'o passageiro é obrigatório'],
  ['destinationId is required', 'o destino é obrigatório'],
  ['currency must be BRL, USD or EUR', 'a moeda deve ser BRL, USD ou EUR'],
];

// Translates one API message; returns the original when it is not a known one
export function translateApiMessage(message: string, lang: 'en' | 'pt'): string {
  if (lang === 'en') return message;
  for (const { pattern, text } of PATTERNS) {
    const match = message.match(pattern);
    if (match) return text.pt(match);
  }
  // "a is required, b must be ..." : translate every known part, keep unknown ones
  const parts = message.split(/,\s*/);
  let known = 0;
  const translated = parts.map((part) => {
    const field = FIELDS.find(([english]) => english === part.trim());
    if (field) known++;
    return field ? field[1] : part;
  });
  return known > 0 ? capitalize(translated.join(', ')) : message;
}

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}
