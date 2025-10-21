-- schema
create table users (
  id bigserial primary key,
  email varchar(120) not null unique,
  password_hash varchar(255) not null
);

create table capacities (
  id bigserial primary key,
  car_type varchar(16) not null unique,
  quantity int not null
);

create table reservations (
  id bigserial primary key,
  user_id bigint not null,
  car_type varchar(16) not null,
  start_at timestamp not null,
  end_at timestamp not null,
  days int not null,
  status varchar(16) not null,
  version bigint
);

create index idx_res_type_window_status on reservations(car_type, start_at, end_at, status);
create index idx_res_user on reservations(user_id);

-- seed capacities (adjust numbers to taste)
insert into capacities(car_type, quantity) values ('SEDAN', 1) on conflict do nothing;
insert into capacities(car_type, quantity) values ('SUV', 1) on conflict do nothing;
insert into capacities(car_type, quantity) values ('VAN', 1) on conflict do nothing;
