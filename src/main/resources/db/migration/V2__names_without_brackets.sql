-- Namen ohne Klammern: Sie stehen in der Auswahl der Muskelteile und in Überschriften, wo der Zusatz
-- in Klammern nur Platz kostet. Der lateinische Name steht ohnehin unter jeder Überschrift.

UPDATE muscle_text SET name = 'Unterarmbeuger'
WHERE muscle_id = 'forearm-flexors' AND language = 'de';
UPDATE muscle_text SET name = 'Unterarmstrecker'
WHERE muscle_id = 'forearm-extensors' AND language = 'de';

UPDATE muscle_text SET name = 'Innerer Quadrizeps'
WHERE muscle_id = 'vastus-medialis' AND language = 'de';
UPDATE muscle_text SET name = 'Vastus Medialis'
WHERE muscle_id = 'vastus-medialis' AND language = 'en';

-- Die beiden Teile der Wade unter ihren eigenen Namen statt "Wade (…)".
UPDATE muscle_text SET name = 'Zwillingswadenmuskel'
WHERE muscle_id = 'gastrocnemius' AND language = 'de';
UPDATE muscle_text SET name = 'Gastrocnemius'
WHERE muscle_id = 'gastrocnemius' AND language = 'en';
UPDATE muscle_text SET name = 'Schollenmuskel'
WHERE muscle_id = 'soleus' AND language = 'de';
UPDATE muscle_text SET name = 'Soleus'
WHERE muscle_id = 'soleus' AND language = 'en';

UPDATE muscle SET latin_name = 'Fibularis longus & brevis'
WHERE id = 'peroneals';

UPDATE exercise_text SET name = 'Unterarmstütz'
WHERE exercise_id = 'plank' AND language = 'de';
