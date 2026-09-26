-- One-off lesson/event signups store "override:<uuid>" in program_id (longer than 36, not a
-- real program row), so the column must be wider and must not enforce the program FK anymore.

-- Drop the (system-named) FK from program_id -> program(id), whatever it is called.
DO $$
DECLARE
    fk_name TEXT;
BEGIN
    SELECT conname INTO fk_name
    FROM pg_constraint
    WHERE conrelid = 'shift_signup'::regclass
      AND contype = 'f'
      AND conkey = ARRAY[(
          SELECT attnum FROM pg_attribute
          WHERE attrelid = 'shift_signup'::regclass AND attname = 'program_id'
      )];
    IF fk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE shift_signup DROP CONSTRAINT %I', fk_name);
    END IF;
END $$;

ALTER TABLE shift_signup ALTER COLUMN program_id TYPE VARCHAR(64);
