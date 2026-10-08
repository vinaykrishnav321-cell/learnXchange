-- LearnXchange database schema (MySQL 8+)
CREATE DATABASE IF NOT EXISTS learnxchange CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE learnxchange;

CREATE TABLE IF NOT EXISTS users (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  department    VARCHAR(100) NOT NULL DEFAULT '',
  bio           VARCHAR(500) NOT NULL DEFAULT '',
  availability  VARCHAR(255) NOT NULL DEFAULT '',
  role          ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
  blocked       BOOLEAN NOT NULL DEFAULT FALSE,
  avg_rating    DECIMAL(3,2) NOT NULL DEFAULT 0,
  rating_count  INT NOT NULL DEFAULT 0,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS skills (
  id       INT AUTO_INCREMENT PRIMARY KEY,
  name     VARCHAR(80) NOT NULL UNIQUE,
  category VARCHAR(60) NOT NULL DEFAULT 'General'
);

CREATE TABLE IF NOT EXISTS user_skills (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  user_id     INT NOT NULL,
  skill_id    INT NOT NULL,
  skill_type  ENUM('TEACH','LEARN') NOT NULL,
  proficiency TINYINT NOT NULL,
  CONSTRAINT chk_prof CHECK (proficiency BETWEEN 1 AND 5),
  CONSTRAINT uq_user_skill UNIQUE (user_id, skill_id, skill_type),
  CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS swap_requests (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  sender_id           INT NOT NULL,
  receiver_id         INT NOT NULL,
  offered_skill_id    INT NOT NULL,
  requested_skill_id  INT NOT NULL,
  message             VARCHAR(500) NOT NULL,
  status              ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_sr_sender   FOREIGN KEY (sender_id)          REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_sr_receiver FOREIGN KEY (receiver_id)        REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_sr_offered  FOREIGN KEY (offered_skill_id)   REFERENCES skills(id),
  CONSTRAINT fk_sr_request  FOREIGN KEY (requested_skill_id) REFERENCES skills(id)
);

CREATE TABLE IF NOT EXISTS sessions (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  request_id       INT NOT NULL,
  session_date     DATE NOT NULL,
  session_time     TIME NOT NULL,
  session_mode     ENUM('ONLINE','OFFLINE') NOT NULL,
  location_or_link VARCHAR(255) NOT NULL,
  status           ENUM('SCHEDULED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_se_request FOREIGN KEY (request_id) REFERENCES swap_requests(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ratings (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  session_id  INT NOT NULL,
  rater_id    INT NOT NULL,
  ratee_id    INT NOT NULL,
  score       TINYINT NOT NULL,
  feedback    VARCHAR(500) NOT NULL DEFAULT '',
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_score CHECK (score BETWEEN 1 AND 5),
  CONSTRAINT uq_rating UNIQUE (session_id, rater_id),
  CONSTRAINT fk_ra_session FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE,
  CONSTRAINT fk_ra_rater   FOREIGN KEY (rater_id)   REFERENCES users(id)    ON DELETE CASCADE,
  CONSTRAINT fk_ra_ratee   FOREIGN KEY (ratee_id)   REFERENCES users(id)    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reports (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  reporter_id  INT NOT NULL,
  reported_id  INT NOT NULL,
  reason       VARCHAR(500) NOT NULL,
  status       ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_rp_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_rp_reported FOREIGN KEY (reported_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Starter skill catalog (admins can add/remove more in the app)
INSERT IGNORE INTO skills (name, category) VALUES
 ('Java','Programming'),('Python','Programming'),('C++','Programming'),('JavaScript','Programming'),
 ('SQL','Programming'),('Web Development','Programming'),('Data Structures','Programming'),
 ('Photoshop','Design'),('Illustrator','Design'),('UI/UX Design','Design'),('Video Editing','Design'),
 ('Public Speaking','Communication'),('Technical Writing','Communication'),('English Conversation','Languages'),
 ('Spanish','Languages'),('French','Languages'),('Hindi','Languages'),
 ('Guitar','Music'),('Piano','Music'),('Singing','Music'),
 ('Excel','Business'),('Digital Marketing','Business'),('Photography','Creative'),
 ('Calculus','Academics'),('Statistics','Academics'),('Physics','Academics');
