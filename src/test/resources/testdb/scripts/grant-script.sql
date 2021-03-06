do
$$
begin

	execute 'grant all on database ' || current_database() || ' to postgres;';

  grant all on schema log TO postgres;
  grant all privileges on all tables in schema log to postgres;
  grant all privileges on all sequences in schema log to postgres;
  grant all privileges on all functions in schema log to postgres;

end
$$ language plpgsql;
