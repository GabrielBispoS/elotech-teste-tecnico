-- usuarios de demonstracao; nao ha endpoint de cadastro no escopo do desafio. Senha: password123
insert into users (name, email, password_hash) values
    ('Ana Souza',    'ana@elotech.com',   '$2a$10$mDVMdtiLBDeRXXKH2Q29He6Tff6uFGoZjxbYv/fhQYR2ZhuLztwna'),
    ('Bruno Lima',   'bruno@elotech.com', '$2a$10$sl.1VOCxAoDBlvyHy6Cu3umorrk0d8HjbqRIVNQ0hrgACU.0BmTQW'),
    ('Carla Dias',   'carla@elotech.com', '$2a$10$CirKE47.MM1qED0khqtL6eQHO8B6Y2FbRx9HZFYpzZuQcEigUCZyq');
