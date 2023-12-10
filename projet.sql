/*
 * @author Gauthier Collard
 * @author Nicolas Heymans
 * @author Martin Quisquater
*/

DROP SCHEMA IF EXISTS Projet_BD2 CASCADE;
CREATE SCHEMA Projet_BD2;

/*********************************************Enums*********************************************/

DROP TYPE IF EXISTS Projet_BD2.SEMESTRES;
DROP TYPE IF EXISTS Projet_BD2.ETATS_STAGE;
DROP TYPE IF EXISTS Projet_BD2.ETATS_CANDIDATURE;

CREATE TYPE Projet_BD2.SEMESTRES AS ENUM ('Q1','Q2');
CREATE TYPE Projet_BD2.ETATS_STAGE AS ENUM ('non validée', 'validée', 'attribuée', 'annulée');
CREATE TYPE Projet_BD2.ETATS_CANDIDATURE AS ENUM ('en attente', 'acceptée', 'refusée', 'annulée');

/*********************************************Create table*********************************************/

CREATE TABLE Projet_BD2.entreprises
(
    nom      VARCHAR(255)        NOT NULL CHECK ( entreprises.nom <> ' ' ),
    code     CHARACTER(3) PRIMARY KEY,
    email    VARCHAR(255) UNIQUE NOT NULL CHECK ( entreprises.email LIKE '%@%' ),
    mdp_hash CHAR(60)            NOT NULL CHECK ( entreprises.mdp_hash <> ' ' ),
    adresse  VARCHAR(255)        NOT NULL CHECK ( entreprises.adresse <> ' ' )
);

CREATE TABLE Projet_BD2.etudiants
(
    id_etudiant       SERIAL PRIMARY KEY,
    nom               VARCHAR(255)         NOT NULL CHECK (etudiants.nom <> ' '),
    prenom            VARCHAR(255)         NOT NULL CHECK (etudiants.prenom <> ' '),
    email             VARCHAR(255) UNIQUE  NOT NULL CHECK (etudiants.email LIKE '%_@student.vinci.be'),
    mdp_hash          CHAR(60)             NOT NULL CHECK (etudiants.mdp_hash <> ' '),
    semestre          Projet_BD2.SEMESTRES NOT NULL,
    nb_candidature_at INTEGER              NOT NULL DEFAULT (0)
);

CREATE TABLE Projet_BD2.mots_clefs
(
    id_mc    SERIAL PRIMARY KEY,
    mot_clef VARCHAR(255) UNIQUE NOT NULL CHECK (mots_clefs.mot_clef <> ' ')
);

CREATE TABLE Projet_BD2.stages
(
    id_stage        SERIAL PRIMARY KEY,
    code_entreprise CHARACTER(3) REFERENCES Projet_BD2.entreprises (code),
    code_stage      VARCHAR(16) UNIQUE     NOT NULL,
    description     VARCHAR(255)           NOT NULL CHECK (stages.description <> ' '),
    semestre        Projet_BD2.SEMESTRES   NOT NULL,
    etat            Projet_BD2.ETATS_STAGE NOT NULL DEFAULT ('non validée')
);

CREATE TABLE Projet_BD2.candidatures
(
    motivation VARCHAR(255)                 NOT NULL CHECK (candidatures.motivation <> ' '),
    etudiant   SERIAL                       NOT NULL REFERENCES Projet_BD2.etudiants (id_etudiant),
    stage      INTEGER                      NOT NULL REFERENCES Projet_BD2.stages (id_stage),
    etat       Projet_BD2.ETATS_CANDIDATURE NOT NULL DEFAULT ('en attente'),
    PRIMARY KEY (stage, etudiant)
);

CREATE TABLE Projet_BD2.stage_mc
(
    stage    INTEGER REFERENCES Projet_BD2.stages (id_stage),
    mot_clef INTEGER REFERENCES Projet_BD2.mots_clefs (id_mc),
    PRIMARY KEY (stage, mot_clef)
);

/*********************************************Application Professeur*********************************************/

/* 1.
Le professeur devra encoder les informations de l'étudiant :
  Son nom,
  Son prénom,
  Son adresse mail (se terminant par @student.vinci.be),
  et le semestre pendant lequel il fera son stage (Q1 ou Q2),
  Il choisira également un mot de passe pour l’étudiant.

Ce mot depasse sera communiqué à l’étudiant par mail.
*/

CREATE
OR REPLACE FUNCTION Projet_BD2.encoderEtudiant(_nom VARCHAR,
                                                      _prenom VARCHAR,
                                                      _email VARCHAR,
                                                      _mdp_hash CHAR(60),
                                                      _semestre CHAR(2)) RETURNS INTEGER AS
$$
DECLARE
id INTEGER;
BEGIN
INSERT INTO Projet_BD2.etudiants(id_etudiant, nom, prenom, email, mdp_hash, semestre, nb_candidature_at)
VALUES (DEFAULT, _ nom, _ prenom, _ email, _ mdp_hash, _semestre :: Projet_BD2.SEMESTRES, DEFAULT) RETURNING id_etudiant
INTO id;
RETURN id;
END;
$$
LANGUAGE plpgsql;

/* 2.
Le professeur devra encoder :
  Le nom de l’entreprise,
  Son adresse (une seule chaîne de caractères),
  Son adresse mail,
  Il choisira pour l’entreprise un identifiant composé de 3 lettres majuscules (par exemple « VIN » pour l’entreprise Vinci),
  Il choisira également un mot de passe pour l’entreprise.

Ce mot de passe sera communiqué à l’entreprise par mail.
 */

CREATE
OR REPLACE FUNCTION Projet_BD2.encoderEntreprise(_nom VARCHAR,
                                                        _code CHARACTER(3),
                                                        _email VARCHAR,
                                                        _mdp_hash CHAR(60),
                                                        _adresse VARCHAR) RETURNS VARCHAR AS
$$
DECLARE
code_entreprise VARCHAR(3);
BEGIN
INSERT INTO Projet_BD2.entreprises(nom, code, email, mdp_hash, adresse)
VALUES (_ nom, _ code, _ email, _ mdp_hash, _ adresse) RETURNING code
INTO code_entreprise;
RETURN code_entreprise;
END ;
$$
LANGUAGE plpgsql;

/* 3.
   Encoder un mot-clé que les entreprises pourront utiliser pour décrire leur stage. Par exemple « Java », « SQL » ou « Web ».

   L’encodage échouera si le mot clé est déjà présent
 */

CREATE
OR REPLACE FUNCTION Projet_BD2.encoderMotClef(_mot_clef VARCHAR) RETURNS INTEGER AS
$$
DECLARE
id INTEGER;
BEGIN
INSERT INTO Projet_BD2.mots_clefs(id_mc, mot_clef)
VALUES (DEFAULT, _ mot_clef) RETURNING id_mc
INTO id;
RETURN id;
END;
$$
LANGUAGE plpgsql;

/* 4.
Voir les offres de stage dans l’état « non validée ».

Pour chaque offre :
   On affichera son code,
   Son semestre,
   Le nom de l’entreprise,
   Sa description.
 */

CREATE
OR REPLACE VIEW Projet_BD2.offresStageNV AS
SELECT st.id_stage, st.code_stage, st.semestre, en.nom AS nom_entreprise, st.description
FROM Projet_BD2.stages st,
     Projet_BD2.entreprises en
WHERE en.code = st.code_entreprise
  AND st.etat = 'non validée'
ORDER BY (en.nom, st.semestre, st.code_stage);

/* 5.
Valider une offre de stage en donnant son code.

On ne pourra valider que des offres de stages « non validée ».
 */

CREATE
OR REPLACE FUNCTION Projet_BD2.validerStage(_code_stage VARCHAR) RETURNS INTEGER AS
$$
DECLARE
_id_stage INTEGER;
BEGIN
    IF
NOT EXISTS(SELECT st.id_stage
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage) THEN
        RAISE 'Code invalide';
END IF;

SELECT st.id_stage
INTO _id_stage
FROM Projet_BD2.stages st
WHERE st.code_stage = _code_stage;

UPDATE Projet_BD2.stages
SET etat = 'validée'
WHERE id_stage = _id_stage;

RETURN
_
id_stage;
END;
$$
LANGUAGE plpgsql;

/* 6.
Voir les offres de stage dans l’état « validée ».

Pour chaque offre :
   On affichera son code,
   Son semestre,
   Le nom de l’entreprise,
   Sa description.
 */

CREATE
OR REPLACE VIEW Projet_BD2.offresStageVA AS
SELECT st.id_stage, st.code_stage, st.semestre, en.nom AS nom_entreprise, st.description
FROM Projet_BD2.stages st,
     Projet_BD2.entreprises en
WHERE en.code = st.code_entreprise
  AND st.etat = 'validée'
ORDER BY (en.nom, st.semestre, st.code_stage);

/* 7.
Voir les étudiants qui n’ont pas de stage (pas de candidature à l’état « acceptée »).

Pour chaque étudiant :
   On affichera son nom,
   Son prénom,
   Son email,
   Le semestre où il fera son stage,
   Le nombre de ses candidatures en attente.
*/

CREATE
OR REPLACE VIEW Projet_BD2.etudiantsSansStage AS
SELECT et.id_etudiant, et.nom, et.prenom, et.email, et.semestre, et.nb_candidature_at
FROM Projet_BD2.etudiants et
WHERE et.id_etudiant NOT IN (SELECT ca.etudiant
                             FROM Projet_BD2.candidatures ca
                             WHERE ca.etat = 'acceptée')
ORDER BY (et.nom, et.prenom);

/* 8
Voir les offres de stage dans l’état « attribuée ».

Pour chaque offre:
   On affichera son code,
   Le nom de l’entreprise,
   Le nom de l’étudiant sélectionné,
   Le prénom de l’étudiant sélectionné.
 */
CREATE
OR REPLACE VIEW Projet_BD2.offresStageAT AS
SELECT et.id_etudiant,
       st.code_stage,
       en.code,
       en.nom    AS nom_entreprise,
       et.nom    AS nom_etudiant,
       et.prenom AS prenom_etudiant
FROM Projet_BD2.entreprises en,
     Projet_BD2.stages st,
     Projet_BD2.candidatures ca,
     Projet_BD2.etudiants et
WHERE en.code = st.code_entreprise
  AND st.id_stage = ca.stage
  AND et.id_etudiant = ca.etudiant
  AND st.etat = 'attribuée'
  AND ca.etat = 'acceptée'
ORDER BY (en.nom, st.code_stage, et.nom, et.prenom);

/*********************************************Application entreprise*********************************************/

/* 1. Encoder une offre de stage.
Pour cela, l’entreprise devra encoder :
   Une description,
   Le semestre concerné.

Chaque offre de stage recevra automatiquement un code qui sera la concaténation de l’identifiant de l’entreprise et d’un numéro.
   Par exemple, le premier stage de l’entreprise Vinci aura le code « VIN1 », le deuxième « VIN2 », le dixième « VIN10 », …

Cette fonctionnalité échouera si l’entreprise a déjà une offre de stage attribuée durant ce semestre.
   */

CREATE
OR REPLACE FUNCTION Projet_BD2.ajouterStage(_code char(3), _description varchar(255), _semestre CHAR(2)) RETURNS INTEGER AS
$$
DECLARE
_id_stage INTEGER;
BEGIN

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, _ code, DEFAULT, _ description, _semestre::Projet_BD2.SEMESTRES, DEFAULT) RETURNING id_stage
INTO _id_stage;

RETURN
_
id_stage;
END;
$$
LANGUAGE plpgsql;


/* 2.
   Voir les mots-clés disponibles pour décrire une offre de stage
*/

CREATE VIEW Projet_BD2.affichageMotsClefs AS
SELECT mc.mot_clef AS "mot"
FROM Projet_BD2.mots_clefs mc
ORDER BY mc.mot_clef;

/* 3.
Ajouter un mot-clé à une de ses offres de stage (en utilisant son code).

Une offre de stage peut avoir au maximum 3 mots-clés.

Ces mots-clés doivent faire partie de la liste des mots-clés proposés par les professeurs.

Il ne sera pas possible d'ajouter un mot-clé :
   Si l'offre de stage est dans l'état "attribuée" ou "annulée"
   Si l’offre n’est pas une offre de l’entreprise.
*/

CREATE
OR REPLACE FUNCTION Projet_BD2.ajouterMotClef(_code_stage VARCHAR(10),
                                                     _mot_clef VARCHAR(255),
                                                     _code_entreprise CHAR(3)) RETURNS INTEGER AS
$$
DECLARE
_id_stage    INTEGER;
    _id_mot_clef INTEGER;
BEGIN

    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage) THEN
        RAISE EXCEPTION 'Cette offre de stage est inexistante';
END IF;

    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage
                    AND st.code_entreprise = _code_entreprise) THEN
        RAISE EXCEPTION 'Le stage n''appartient pas a l''entreprise';
END IF;


    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.mots_clefs mc
                  WHERE mc.mot_clef = _mot_clef) THEN
        RAISE EXCEPTION 'Le mot clef n''existe pas';
END IF;

SELECT st.id_stage
into _id_stage
FROM projet_bd2.stages st
WHERE st.code_stage = _code_stage
  AND st.code_entreprise = _code_entreprise;

SELECT mc.id_mc
into _id_mot_clef
FROM projet_bd2.mots_clefs mc
WHERE mc.mot_clef = _mot_clef;

INSERT INTO Projet_BD2.stage_mc(stage, mot_clef)
VALUES (_ id_stage, _ id_mot_clef) RETURNING mot_clef
INTO _id_mot_clef;

RETURN
_
id_mot_clef;
END
$$
LANGUAGE plpgsql;

/* 4.
Pour chaque offre de stage, on affichera :
   Son code,
   Sa description,
   Son semestre,
   Son état,
   Le nombre de candidatures en attente (pour ce stage),
   Et le nom de l’étudiant qui fera le stage (si l’offre a déjà été attribuée).

Si l'offre de stage n'a pas encore été attribuée, il sera indiqué "pas attribuée" à la place du nom de l'étudiant. */

CREATE
OR REPLACE VIEW Projet_BD2.voirOffresDeStages AS
WITH nbCandidatureAt AS (SELECT st.id_stage, COUNT(ca.etudiant) AS "nb_candidature_at"
                         FROM Projet_BD2.stages st
                                  LEFT OUTER JOIN Projet_BD2.candidatures ca ON st.id_stage = ca.stage
                             AND ca.etat = 'en attente'
                         GROUP BY st.id_stage),

     listeEtudiantAccepte AS (SELECT st.id_stage, COALESCE(et.nom, 'pas attribuée') AS "nom_etudiant"
                              FROM Projet_BD2.stages st
                                       LEFT OUTER JOIN Projet_BD2.candidatures ca
                                                       ON st.id_stage = ca.stage
                                                           AND ca.etat = 'acceptée'
                                       LEFT OUTER JOIN Projet_BD2.etudiants et
                                                       ON ca.etudiant = et.id_etudiant
                                                           AND ca.etat = 'acceptée')

SELECT st.code_entreprise,
       st.code_stage,
       st.description,
       st.semestre,
       st.etat,
       nca.nb_candidature_at,
       lea.nom_etudiant
FROM Projet_BD2.stages st,
     nbCandidatureAt ncA,
     listeEtudiantAccepte lea
WHERE st.id_stage = nca.id_stage
  AND st.id_stage = lea.id_stage
ORDER BY st.code_entreprise, st.semestre, st.code_stage;

/* 5.
Voir les candidatures pour une de ses offres de stages en donnant son code.

Pour chaque candidature :
   On affichera son état,
   Le nom,
   Le prénom,
   L'adresse mail,
   Les motivations de l’étudiant.

Si le code ne correspond pas à une offre de l’entreprise ou qu’il n’y a pas de candidature pour cette offre, le message suivant sera affiché :
   “Il n'y a pas de candidature pour cette offre ou vous n'avez pas d'offre ayant ce code”. (coté java)
*/

CREATE
OR REPLACE VIEW Projet_BD2.voirCandidatures AS
SELECT et.id_etudiant,
       st.id_stage,
       st.code_stage,
       ca.etat,
       et.nom,
       et.prenom,
       et.email,
       ca.motivation
FROM Projet_BD2.candidatures ca,
     Projet_BD2.etudiants et,
     Projet_BD2.stages st
WHERE ca.etudiant = et.id_etudiant
  AND ca.stage = st.id_stage
ORDER BY st.code_stage, et.nom, et.prenom;

/* 6.
Sélectionner un étudiant pour une de ses offres de stage. Pour cela, l’entreprise devra
donner le code de l’offre et l’adresse mail de l’étudiant.

L’état de l’offre passera à « attribuée ».

La candidature de l’étudiant passera à l’état « acceptée ».

L’opération échouera :
   Si l’offre de stage n’est pas une offre de l’entreprise
   Si l’offre n’est pas dans l’état « validée » ou que la candidature n’est pas dans l’état « en attente ».

Les autres candidatures en attente de cet étudiant passeront à l’état « annulée ».

Les autres candidatures en attente d’étudiants pour cette offre passeront à « refusée ».

Si l’entreprise avait d’autres offres de stage non annulées durant ce semestre :
   l’état de celles-ci doit passer à « annulée »
   toutes les candidatures en attente de ces offres passeront à « refusée »
   */

CREATE
OR REPLACE FUNCTION Projet_BD2.selectionnerEtudiant(_code_stage VARCHAR(10),
                                                           _email VARCHAR(255),
                                                           _code_entreprise CHAR(3)) RETURNS INTEGER AS
$$
DECLARE
_id_etudiant INTEGER;
    _id_stage    INTEGER;
BEGIN
    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage) THEN
        RAISE EXCEPTION 'Cette offre de stage n''existe pas';
END IF;

SELECT st.id_stage
INTO _id_stage
FROM Projet_BD2.stages st
WHERE st.code_stage = _code_stage;

IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.etudiants et
                  WHERE et.email = _email) THEN
        RAISE EXCEPTION 'Cet étudiant n''existe pas';
END IF;

SELECT et.id_etudiant
INTO _id_etudiant
FROM Projet_BD2.etudiants et
WHERE et.email = _email;

IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.id_stage = _id_stage
                    AND st.code_entreprise = _code_entreprise) THEN
        RAISE EXCEPTION 'Le stage n''appartient pas a l''entreprise';
END IF;

    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.candidatures ca
                  WHERE ca.stage = _id_stage
                    AND ca.etudiant = _id_etudiant) THEN
        RAISE EXCEPTION 'La candidature n''existe pas';
END IF;

UPDATE Projet_BD2.candidatures ca
SET etat = 'acceptée'
WHERE ca.etudiant = _id_etudiant
      AND ca.stage = _id_stage;

RETURN
_
id_etudiant;
END
$$
LANGUAGE plpgsql;

/* 7.
Annuler une offre de stage en donnant son code.

Cette opération ne pourra être réalisée que si l’offre appartient bien à l’entreprise et si:
   Elle n’est pas encore attribuée,
   Ni annulée.

Toutes les candidatures en attente de cette offre passeront à « refusée »
   */

CREATE
OR REPLACE FUNCTION Projet_BD2.annulerOffreStage(_code_stage varchar(10), _code_entreprse char(3)) RETURNS BOOLEAN AS
$$
DECLARE
BEGIN
    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage) THEN
        RAISE EXCEPTION 'Cette offre de stage n''existe pas';
END IF;

    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_stage
                    AND st.code_entreprise = _code_entreprse) THEN
        RAISE EXCEPTION 'Ce stage ne vient pas de votre entreprise';
END IF;

UPDATE Projet_BD2.stages st
SET etat = 'annulée'
WHERE st.code_stage = _code_stage;

RETURN TRUE;
END;
$$
LANGUAGE plpgsql;

/*********************************************Application Etudiant*********************************************/

/*1.
Voir toutes les offres de stage dans l’état « validée » correspondant au semestre où l’étudiant fera son stage.

Pour une offre de stage :
  On affichera son code,
  Le nom de l’entreprise,
  Son adresse,
  Sa description,
  Les mots-clés (séparés par des virgules sur une même ligne).
 */

CREATE
OR REPLACE VIEW Projet_BD2.offresStageValideesParEtudiant AS

WITH offresStageValidees AS (SELECT st.code_stage,
                                    en.nom,
                                    en.adresse,
                                    st.description,
                                    COALESCE(STRING_AGG(mc.mot_clef, ', '), 'Pas de mots clefs') AS mots_clefs,
                                    st.semestre
                             FROM Projet_BD2.entreprises en,
                                  Projet_BD2.stages st
                                      LEFT OUTER JOIN Projet_BD2.stage_mc smc ON st.id_stage = smc.stage
                                      LEFT OUTER JOIN Projet_BD2.mots_clefs mc ON smc.mot_clef = mc.id_mc
                             WHERE en.code = st.code_entreprise
                               AND st.etat = 'validée'
                             GROUP BY st.code_stage, en.nom, en.adresse, st.description, st.semestre
                             ORDER BY st.code_stage)

SELECT et.id_etudiant, osv.code_stage, osv.nom, osv.adresse, osv.description, osv.mots_clefs
FROM offresStageValidees osv,
     Projet_BD2.etudiants et
WHERE osv.semestre = et.semestre
ORDER BY osv.nom, osv.code_stage;

/*2.
Recherche d’une offre de stage par mot clé.

Cette recherche n’affichera que les offres de stages validées et correspondant au semestre où l’étudiant fera son stage.

Pour une offre de stage :
  On affichera son code,
  Le nom de l’entreprise,
  Son adresse,
  Sa description,
  Les mots-clés (séparés par des virgules sur une même ligne).
*/

CREATE
OR REPLACE VIEW Projet_BD2.rechercheOffresStageMotClef AS

WITH offresStageValidees AS (SELECT st.code_stage,
                                    en.nom,
                                    en.adresse,
                                    st.description,
                                    COALESCE(STRING_AGG(mc.mot_clef, ', '), 'Pas de mots clef') AS mots_clefs,
                                    st.semestre
                             FROM Projet_BD2.entreprises en,
                                  Projet_BD2.stages st
                                      LEFT OUTER JOIN Projet_BD2.stage_mc smc ON st.id_stage = smc.stage
                                      LEFT OUTER JOIN Projet_BD2.mots_clefs mc ON smc.mot_clef = mc.id_mc
                             WHERE en.code = st.code_entreprise
                               AND st.etat = 'validée'
                             GROUP BY st.code_stage, en.nom, en.adresse, st.description, st.semestre
                             ORDER BY st.code_stage),
     listMotClef AS (SELECT st.code_stage, mc.mot_clef
                     FROM Projet_BD2.mots_clefs mc,
                          Projet_BD2.stage_mc smc,
                          Projet_BD2.stages st
                     WHERE st.id_stage = smc.stage
                       AND smc.mot_clef = mc.id_mc)

SELECT et.id_etudiant, osv.code_stage, osv.nom, osv.adresse, osv.description, lmc.mot_clef
FROM offresStageValidees osv,
     listMotClef lmc,
     Projet_BD2.etudiants et
WHERE osv.semestre = et.semestre
  AND lmc.code_stage = osv.code_stage
ORDER BY osv.nom, osv.code_stage;

/*3.
Poser sa candidature.

Pour cela, il doit donner :
  Le code de l’offre de stage,
  Ses motivations sous format textuel.

Il ne peut poser de candidature :
  S’il a déjà une candidature acceptée,
  S’il a déjà posé sa candidature pour cette offre,
  Si l’offre n’est pas dans l’état "validée"
  Si l’offre ne correspond pas au bon semestre.
*/

CREATE
OR REPLACE FUNCTION Projet_BD2.poserCandidature(_code_offre_stage VARCHAR(10),
                                                       _motivations VARCHAR(255),
                                                       _id_etudiant INTEGER) RETURNS INTEGER AS
$$
DECLARE
_id_stage INTEGER;
BEGIN
    IF
NOT EXISTS(SELECT st.code_stage
                  FROM Projet_BD2.stages st
                  WHERE st.code_stage = _code_offre_stage) THEN
        RAISE 'Code d''offre de stage inexistant';
end if;

    IF
NOT EXISTS(WITH stageSemestre AS (SELECT st.id_stage, st.code_stage, st.semestre
                                         FROM Projet_BD2.stages st),
                       etudiantSemestr AS (SELECT et.id_etudiant, et.nom, et.prenom, et.semestre
                                           FROM Projet_BD2.etudiants et)

                  SELECT *
                  FROM etudiantSemestr ets,
                       stageSemestre sts
                  WHERE ets.semestre = sts.semestre
                    AND sts.code_stage = _code_offre_stage
                    AND ets.id_etudiant = _id_etudiant) THEN
        RAISE 'Le quadrimestre du stage ne correspond pas au quadrimestre de l''étudiant';
END IF;

SELECT st.id_stage
INTO _id_stage
FROM Projet_BD2.stages st
WHERE st.code_stage = _code_offre_stage;

INSERT INTO Projet_BD2.candidatures(motivation, etudiant, stage, etat)
VALUES (_ motivations, _ id_etudiant, _ id_stage, DEFAULT);

RETURN
_
id_stage;
END
$$
LANGUAGE plpgsql;

/*4.
Voir les offres de stage pour lesquelles l’étudiant a posé sa candidature.

Pour chaque offre:
  On verra le code de l’offre,
  Le nom de l’entreprise,
  L’état de sa candidature.
*/
CREATE
OR REPLACE VIEW Projet_BD2.offresCandidature AS
SELECT st.id_stage, ca.etudiant, st.code_stage, en.nom, ca.etat
FROM Projet_BD2.candidatures ca,
     Projet_BD2.stages st,
     Projet_BD2.entreprises en
WHERE ca.stage = st.id_stage
  AND st.code_entreprise = en.code
ORDER BY en.nom, st.code_stage;

/*5.
Annuler une candidature en précisant le code de l’offre de stage.

Les candidatures ne peuvent être annulées que si elles sont « en attente ».
 */

CREATE
OR REPLACE FUNCTION Projet_BD2.annulerCandidature(_code_stage VARCHAR, _id_etudiant INTEGER) RETURNS INTEGER AS
$$
DECLARE
_id_stage INTEGER;
BEGIN
    IF
NOT EXISTS(SELECT *
                  FROM Projet_BD2.stages st,
                       Projet_BD2.candidatures ca
                  WHERE st.id_stage = ca.stage
                    AND st.code_stage = _code_stage
                    AND ca.etudiant = _id_etudiant) THEN
        RAISE 'Pas de candidature pour ce stage !';
END IF;

SELECT st.id_stage
INTO _id_stage
FROM Projet_BD2.stages st
WHERE st.code_stage = _code_stage;

UPDATE Projet_BD2.candidatures ca
SET etat = 'annulée'
WHERE ca.stage = _id_stage
      AND ca.etudiant = _id_etudiant;

RETURN
_
id_stage;
END;
$$
LANGUAGE plpgsql;

/*********************************************Trigger*********************************************/

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationInsertCandidature() RETURNS TRIGGER AS
$$
DECLARE

BEGIN
    -- Il ne peut poser de candidature : s’il a déjà une candidature acceptée,
    IF
EXISTS(SELECT *
              FROM Projet_BD2.candidatures c
              WHERE c.etudiant = NEW.etudiant
                AND c.etat = 'acceptée') THEN
        RAISE 'Candidature déjà acceptée pour cet étudiant !';
END IF;

    -- Il ne peut poser de candidature : s’il a déjà posé sa candidature pour cette offre,
    IF
EXISTS(SELECT *
              FROM Projet_BD2.candidatures c
              WHERE c.etudiant = NEW.etudiant
                AND c.stage = NEW.stage) THEN
        RAISE 'Candidature déjà posée pour cette offre !';
END IF;

    -- Il ne peut poser de candidature : si l’offre n’est pas dans l’état validée
    IF
EXISTS(SELECT *
              FROM Projet_BD2.stages st
              WHERE st.id_stage = NEW.stage
                AND st.etat != 'validée') THEN
        RAISE 'Offre de stage correspondante dans un autre etat que validée !';
end if;

    -- Il ne peut poser de candidature : si l’offre ne correspond pas au bon semestre.
    IF
EXISTS(SELECT *
              FROM Projet_BD2.stages st,
                   Projet_BD2.candidatures ca,
                   Projet_BD2.etudiants et
              WHERE st.id_stage = ca.stage
                AND ca.etudiant = et.id_etudiant
                AND ca.etudiant = NEW.etudiant
                AND ca.stage = NEW.stage
                AND st.semestre != et.semestre) THEN
        RAISE 'Quadrimestre non correspondant !';
END IF;

RETURN NEW;
END ;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerInsertCandidature
    BEFORE INSERT
    ON Projet_BD2.candidatures
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationInsertCandidature();

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationInsertCandidatureAfter() RETURNS TRIGGER AS
$$
DECLARE
_nb_candidature_at INTEGER;
BEGIN

SELECT COUNT(ca.*)
INTO _nb_candidature_at
FROM Projet_BD2.candidatures ca
WHERE ca.etudiant = NEW.etudiant
  AND ca.etat = 'en attente';

UPDATE Projet_BD2.etudiants et
SET nb_candidature_at = _nb_candidature_at
    WHERE et.id_etudiant = NEW.etudiant;

RETURN NEW;
END ;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerInsertCandidatureAfter
    AFTER INSERT
    ON Projet_BD2.candidatures
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationInsertCandidatureAfter();

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationEtatCandidature() RETURNS TRIGGER AS
$$
DECLARE
_nb_candidature_at INTEGER;
BEGIN

    -- Les candidatures ne peuvent être annulées que si elles sont « en attente ».
-- L’opération échouera : si la candidature n’est pas dans l’état « en attente ».
    IF
(OLD.etat != 'en attente' AND NEW.etat = 'annulée')
    THEN
        RAISE EXCEPTION 'Une candidature qui n''est pas en attente ne peut être annulée !';
END IF;

-- dis null part mais dois quand meme ete la
    IF
(OLD.etat != 'en attente' AND NEW.etat = 'acceptée')
    THEN
        RAISE EXCEPTION 'Une candidature qui n''est pas en attente ne peut être acceptée !';
END IF;

-- dis null part mais dois quand meme ete la
    IF
(OLD.etat = 'acceptée' AND NEW.etat = 'refusée')
    THEN
        RAISE EXCEPTION 'Une candidature qui n''est pas acceptée ne peux etre refusée !';
END IF;

    IF
(NEW.etat = 'acceptée')
    THEN

-- L’état de l’offre passera à « attribuée ».
UPDATE Projet_BD2.stages st
SET etat = 'attribuée'
WHERE st.id_stage = NEW.stage;

-- Les candidatures en attente de cet étudiant passeront à l’état « annulée ».
UPDATE Projet_BD2.candidatures ca
SET etat = 'annulée'
WHERE ca.etudiant = NEW.etudiant
  AND ca.stage != NEW.stage
          AND ca.etat = 'en attente';

-- Les autres candidatures en attente d’étudiants pour cette offre passeront à « refusée ».
UPDATE Projet_BD2.candidatures ca
SET etat = 'refusée'
WHERE ca.etudiant != NEW.etudiant
          AND ca.stage = NEW.stage
          AND ca.etat = 'en attente';
END IF;

SELECT COUNT(ca.*)
INTO _nb_candidature_at
FROM Projet_BD2.candidatures ca
WHERE ca.etudiant = NEW.etudiant
  AND ca.etat = 'en attente';

UPDATE Projet_BD2.etudiants et
SET nb_candidature_at = _nb_candidature_at
    WHERE et.id_etudiant = NEW.etudiant;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerEtatCandidature
    AFTER UPDATE OF etat
    ON Projet_BD2.candidatures
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationEtatCandidature();

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationInsertStage() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    -- Cette fonctionnalité échouera si l’entreprise a déjà une offre de stage attribuée durant ce semestre.
    IF
EXISTS(SELECT *
              FROM Projet_BD2.stages st
              WHERE st.code_entreprise = NEW.code_entreprise
                AND st.semestre = NEW.semestre
                AND st.etat = 'attribuée') THEN
        RAISE EXCEPTION 'Un stage de cette entreprise est deja attribué pour ce semestre';
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerInsertStage
    AFTER INSERT
    ON Projet_BD2.stages
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationInsertStage();


CREATE
OR REPLACE FUNCTION Projet_BD2.verificationEtatStage() RETURNS TRIGGER AS
$$
DECLARE
BEGIN

    -- On ne pourra valider que des offres de stages « non validée »
    IF
(OLD.etat != 'non validée' AND NEW.etat = 'validée')
    THEN
        RAISE EXCEPTION 'Un stage dans un etat different de non validée ne peut etre validée !';
END IF;

-- L’opération échouera si : l’offre n’est pas dans l’état « validée ».
    IF
(OLD.etat != 'validée' AND NEW.etat = 'attribuée')
    THEN
        RAISE EXCEPTION 'Un stage dans un etat different de non validée ne peut etre validée ! ';
END IF;

    IF
(OLD.etat = 'annulée' AND NEW.etat = 'attribuée')
    THEN
        RAISE EXCEPTION 'Un stage dans un etat annulée ne peut etre attribuée !';
END IF;

-- dis null part mais dois quand meme ete la
    IF
(OLD.etat = 'annulée' AND NEW.etat = 'annulée')
    THEN
        RAISE EXCEPTION 'Un stage dans un etat annulée ne peut etre annulée !';
END IF;

    IF
(NEW.etat = 'attribuée')
    THEN

-- Si l’entreprise avait d’autres offres de stage dans un autre état que annulée durant ce semestre, l’état de celles-ci doit passer à « annulée ».
UPDATE Projet_BD2.stages st
SET etat = 'annulée'
WHERE st.code_entreprise = NEW.code_entreprise
  AND st.etat != 'attribuée'
          AND st.semestre IN (SELECT st1.semestre
                              FROM Projet_BD2.stages st1
                              WHERE st1.code_entreprise = NEW.code_entreprise
                                AND st1.etat = 'attribuée');
END IF;

-- Toutes les candidatures en attente de cette offre passeront à « refusée »
    IF
(NEW.etat = 'annulée')
    THEN

-- Toutes les candidatures en attente de ces offres passeront à « refusée »
UPDATE Projet_BD2.candidatures ca
SET etat = 'refusée'
WHERE ca.stage = NEW.id_stage
  AND ca.etat = 'en attente';
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerEtatStage
    AFTER UPDATE OF etat
    ON Projet_BD2.stages
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationEtatStage();

CREATE
OR REPLACE FUNCTION Projet_BD2.creerCodeStage() RETURNS TRIGGER AS
$$
DECLARE
_code_stage VARCHAR(5);
    _index      INTEGER;
BEGIN

SELECT COUNT(*)
INTO _ index
FROM Projet_BD2.stages st
WHERE st.code_entreprise = NEW.code_entreprise;

_code_stage = CONCAT(NEW.code_entreprise, _index + 1);
    NEW.code_stage
= _code_stage;
RETURN NEW;

END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerCodeStage
    BEFORE INSERT
    ON Projet_BD2.stages
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.creerCodeStage();

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationInsertStageMC() RETURNS TRIGGER AS
$$
DECLARE
BEGIN
    -- Il sera impossible d'ajouter un mot-clé si : l'offre de stage est dans l'état "attribuée" ou "annulée"
    IF
EXISTS(SELECT *
              FROM Projet_BD2.stages st
              WHERE st.id_stage = NEW.stage
                AND (st.etat = 'attribuée' OR st.etat = 'annulée')) THEN
        RAISE EXCEPTION 'Impossible d''ajouter un mot-clé, le stage est dans l''état "attribuée" ou "annulée"';
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerInsertStageMC
    AFTER INSERT
    ON Projet_BD2.stage_mc
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationInsertStageMC();

CREATE
OR REPLACE FUNCTION Projet_BD2.verificationMaxtroisMotsClefs() RETURNS TRIGGER AS
$$
DECLARE
_index INTEGER;
BEGIN
    /* Une offre de
    stage peut avoir au maximum 3 mots-clés. Ces mots-clés doivent faire partie de la liste
    des mots-clés proposés par les professeurs. */
SELECT COUNT(*)
INTO _ index
FROM Projet_BD2.stage_mc st_mc
WHERE st_mc.stage = NEW.stage;

IF
_index > 3 THEN
        RAISE EXCEPTION 'Une offre de stage peut avoir au maximum 3 mots-clés';
END IF;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER triggerMaxTroisMotsClefs
    AFTER INSERT
    ON Projet_BD2.stage_mc
    FOR EACH ROW
    EXECUTE PROCEDURE Projet_BD2.verificationMaxtroisMotsClefs();

/*********************************************Insert*********************************************/

INSERT INTO Projet_BD2.etudiants(id_etudiant, nom, prenom, email, mdp_hash, semestre, nb_candidature_at)
VALUES (DEFAULT, 'De', 'Jean', 'j.d@student.vinci.be', '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um',
        'Q2', DEFAULT);

INSERT INTO Projet_BD2.etudiants(id_etudiant, nom, prenom, email, mdp_hash, semestre, nb_candidature_at)
VALUES (DEFAULT, 'Du', 'Marc', 'm.d@student.vinci.be', '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um',
        'Q1', DEFAULT);

INSERT INTO Projet_BD2.mots_clefs(id_mc, mot_clef)
VALUES (DEFAULT, 'Java');

INSERT INTO Projet_BD2.mots_clefs(id_mc, mot_clef)
VALUES (DEFAULT, 'Web');

INSERT INTO Projet_BD2.mots_clefs(id_mc, mot_clef)
VALUES (DEFAULT, 'Python');

INSERT INTO Projet_BD2.entreprises(nom, code, email, mdp_hash, adresse)
VALUES ('VINCI', 'VIN', 'vinci@vinci.be', '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um',
        'La rue de vinci');

INSERT INTO Projet_BD2.entreprises(nom, code, email, mdp_hash, adresse)
VALUES ('ULB', 'ULB', 'ulb@ulb.be', '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um',
        'La rue de l ulb');

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'VIN', DEFAULT, 'stage SAP', 'Q2', 'validée');

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'VIN', DEFAULT, 'stage BI', 'Q2', DEFAULT);

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'VIN', DEFAULT, 'stage Unity', 'Q2', DEFAULT);

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'VIN', DEFAULT, 'stage IA', 'Q2', 'validée');

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'VIN', DEFAULT, 'stage Mobile', 'Q1', 'validée');

INSERT INTO Projet_BD2.stages(id_stage, code_entreprise, code_stage, description, semestre, etat)
VALUES (DEFAULT, 'ULB', DEFAULT, 'stage Java Scripte', 'Q2', 'validée');

INSERT INTO Projet_BD2.stage_mc(stage, mot_clef)
VALUES (3, 1);

INSERT INTO Projet_BD2.stage_mc(stage, mot_clef)
VALUES (5, 1);

INSERT INTO Projet_BD2.candidatures(motivation, etudiant, stage, etat)
VALUES ('Je suis chaud', 1, 4, DEFAULT);

INSERT INTO Projet_BD2.candidatures(motivation, etudiant, stage, etat)
VALUES ('Je suis tres chaud', 2, 5, DEFAULT);

/*********************************************Grant*********************************************/

GRANT
CONNECT
ON DATABASE dbnicolasheymans TO martinquisquater, gauthiercollard;

GRANT USAGE ON SCHEMA
Projet_BD2 TO nicolasheymans, martinquisquater, gauthiercollard;

-- Application Entreprise
GRANT INSERT ON Projet_BD2.stages
,
    Projet_BD2.stage_mc
    TO martinquisquater;

GRANT
SELECT
ON Projet_BD2.stages,
    Projet_BD2.stage_mc,
    Projet_BD2.mots_clefs,
    Projet_BD2.candidatures,
    Projet_BD2.etudiants,
    Projet_BD2.entreprises,
    Projet_BD2.affichageMotsClefs,
    Projet_BD2.voirOffresDeStages,
    Projet_BD2.voirCandidatures
    TO martinquisquater;

GRANT
UPDATE ON Projet_BD2.candidatures,
    Projet_BD2.stages,
    Projet_BD2.etudiants
    TO martinquisquater;

GRANT USAGE, SELECT ON SEQUENCE Projet_BD2.stages_id_stage_seq
    TO martinquisquater;

-- Application Etudiant
GRANT INSERT ON Projet_BD2.candidatures
    TO gauthiercollard;

GRANT
SELECT
ON Projet_BD2.stages,
    Projet_BD2.entreprises,
    Projet_BD2.stage_mc,
    Projet_BD2.mots_clefs,
    Projet_BD2.candidatures,
    Projet_BD2.etudiants,
    Projet_BD2.offresStageValideesParEtudiant,
    Projet_BD2.rechercheOffresStageMotClef,
    Projet_BD2.offresCandidature
    TO gauthiercollard;

GRANT
UPDATE ON Projet_BD2.candidatures,
    Projet_BD2.etudiants
    TO gauthiercollard;

/*********************************************Test*********************************************/
/*
----1
--1.a
SELECT Projet_BD2.encoderEtudiant('Pe', 'Luc', 'l.p@student.vinci.be',
                                  '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um', 'Q2');
--1.b
SELECT Projet_BD2.encoderEntreprise('UCL', 'UCL', 'ucl@ucl.be',
                                    '$2a$10$3k9iGHAGLw7wTT.g95gCF.p0Gp4ymcAobR.XJYqAstDb7Aa8gw9um', 'adresse');

----2
--2.a
SELECT Projet_BD2.ajouterStage('UCL', 'Stage SQL', 'Q2');
--2.b
SELECT Projet_BD2.ajouterStage('UCL', 'Stage ODOO', 'Q1');
--2.c
SELECT *
FROM Projet_BD2.affichageMotsClefs;
--2.d
SELECT Projet_BD2.ajouterMotClef('UCL1', 'Java', 'UCL');
--2.e
SELECT Projet_BD2.ajouterMotClef('UCL1', 'Web', 'UCL');
--2.f
SELECT Projet_BD2.ajouterMotClef('UCL1', 'SQL', 'UCL');
--2.g
SELECT Projet_BD2.ajouterMotClef('UCL1', 'Java', 'UCL');
--2.h
SELECT Projet_BD2.ajouterMotClef('VIN1', 'Java', 'UCL');
--2.i
SELECT *
FROM Projet_BD2.voirOffresDeStages
WHERE code_entreprise = 'UCL';
--2.j
SELECT Projet_BD2.selectionnerEtudiant('UCL1', 'j.d@student.vinci.be', 'UCL');

----3
--3.a
SELECT *
FROM Projet_BD2.offresStageNV;
--3.b
SELECT Projet_BD2.validerStage('VIN2');
--3.c
SELECT Projet_BD2.validerStage('UCL1');
--3.d
SELECT Projet_BD2.validerStage('UCL3');
--3.e
SELECT *
FROM Projet_BD2.offresStageVA;
--3.f
SELECT Projet_BD2.encoderMotClef('Java');
--3.g
SELECT Projet_BD2.encoderMotClef('SQL');

----4
--4.a
SELECT Projet_BD2.ajouterMotClef('UCL1', 'Python', 'UCL');
--4.b
SELECT Projet_BD2.ajouterMotClef('UCL1', 'SQL', 'UCL');

----5
--5.a
SELECT *
FROM Projet_BD2.offresStageValideesParEtudiant
WHERE id_etudiant = 1;
--5.b
SELECT *
FROM Projet_BD2.rechercheOffresStageMotClef
WHERE id_etudiant = 1
  AND mot_clef = 'Java';
--5.c
SELECT Projet_BD2.poserCandidature('VIN1', '5.c', 1);
--5.d
SELECT Projet_BD2.poserCandidature('VIN1', '5.d', 1);
--5.e
SELECT Projet_BD2.poserCandidature('VIN2', '5.e', 1);
--5.f
SELECT Projet_BD2.poserCandidature('VIN3', '5.f', 1);
--5.g
SELECT Projet_BD2.poserCandidature('UCL1', '5.g', 1);

----6
--6.a
SELECT Projet_BD2.poserCandidature('VIN1', '6.a', 3);
--6.b
SELECT Projet_BD2.poserCandidature('VIN4', '6.b', 3);
--6.c
SELECT Projet_BD2.poserCandidature('UCL1', '6.c', 3);

----7
--7.a
SELECT Projet_BD2.poserCandidature('VIN1', '6.a', 2);

----8
--8.a
SELECT *
FROM Projet_BD2.etudiantsSansStage;

----9
--9.a
SELECT *
FROM Projet_BD2.voirOffresDeStages
WHERE code_entreprise = 'VIN';
--9.b
SELECT Projet_BD2.annulerOffreStage('UCL1', 'VIN');
--9.c
SELECT *
FROM Projet_BD2.voirCandidatures
WHERE code_stage = 'UCL1';
--9.d
SELECT *
FROM Projet_BD2.voirCandidatures
WHERE code_stage = 'VIN1';
--9.e
SELECT Projet_BD2.selectionnerEtudiant('UCL1', 'j.d@student.vinci.be', 'VIN');
--9.f
SELECT Projet_BD2.selectionnerEtudiant('VIN1', 'j.d@student.vinci.be', 'VIN');
--9.g

SELECT *
FROM Projet_BD2.voirOffresDeStages
WHERE code_entreprise = 'VIN';
--9.h
SELECT Projet_BD2.selectionnerEtudiant('VIN1', 'l.p@student.vinci.be', 'VIN');
--9.i
SELECT *
FROM Projet_BD2.voirOffresDeStages
WHERE code_entreprise = 'VIN';
--9.j
SELECT *
FROM Projet_BD2.voirCandidatures
WHERE code_stage = 'VIN1';
--9.k
SELECT *
FROM Projet_BD2.voirCandidatures
WHERE code_stage = 'VIN5';
--9.l
SELECT Projet_BD2.annulerOffreStage('VIN5', 'VIN');
--9.m
SELECT Projet_BD2.ajouterMotClef('VIN5', 'Web', 'VIN');
--9.n
SELECT *
FROM Projet_BD2.voirCandidatures
WHERE code_stage = 'VIN5';
--9.o
SELECT Projet_BD2.annulerOffreStage('VIN3', 'VIN');
--9.p
SELECT Projet_BD2.annulerOffreStage('VIN6', 'VIN');
--9.q
SELECT Projet_BD2.ajouterStage('VIN', '9.q', 'Q2');

----10
--10.a
SELECT *
FROM Projet_BD2.offresCandidature
WHERE etudiant = 1;
--10.b
SELECT Projet_BD2.poserCandidature('ULB1', '10.b', 1);
--10.c
SELECT Projet_BD2.annulerCandidature('VIN1', 1);
--10.d
SELECT *
FROM Projet_BD2.offresCandidature
WHERE etudiant = 1;

----11
--11.a
SELECT Projet_BD2.poserCandidature('VIN2', '11.a', 3);
--11.b
SELECT *
FROM Projet_BD2.offresCandidature
WHERE etudiant = 3;
--11.c
SELECT Projet_BD2.annulerCandidature('UCL1', 3);
--11.d
SELECT *
FROM Projet_BD2.offresCandidature
WHERE etudiant = 3;

----12
--12.a
SELECT Projet_BD2.selectionnerEtudiant('UCL1', 'l.p@student.vinci.be', 'UCL');

----13
--13.a
SELECT *
FROM Projet_BD2.etudiantsSansStage;
--13.b
SELECT *
FROM Projet_BD2.offresStageAT;
--13.c
SELECT Projet_BD2.validerStage('VIN1');
*/