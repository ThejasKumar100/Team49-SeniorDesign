# Team49-SeniorDesign

Class: CS 4485.0W1 - S26

Authors:
- Thejaswin Kumaran
- Vaishnavi Pasumarthi
- Rochan Vanam
- Mytri Nai
- Kaavya Jampana
- Blessing Ogunfowora

Faculty Sposor: John Cole


# Set up 

# 1. Install MySQL Server

## Option A (Recommended - Mac)

Download MySQL Community Server:
https://dev.mysql.com/downloads/mysql/

Install using the `.pkg` file.

During installation:

* Set a root password
* Make sure the server is started

---

## Verify MySQL

Open Terminal and run:

```bash
mysql --version
mysql -u root -p
```

If login works, MySQL is ready.

---

# 2. Create Database

Inside MySQL:

```sql
CREATE DATABASE sentence_builder;
USE sentence_builder;
```

---

# 3. Create Tables

Run:

```sql
CREATE TABLE Books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    word_count INT NOT NULL,
    imported_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Words (
    word_id INT AUTO_INCREMENT PRIMARY KEY,
    word_text VARCHAR(100) NOT NULL UNIQUE,
    total_occurrences INT DEFAULT 0,
    start_count INT DEFAULT 0,
    end_count INT DEFAULT 0,
    can_start BOOLEAN DEFAULT FALSE,
    can_end BOOLEAN DEFAULT FALSE
);

CREATE TABLE Word_Followers (
    relation_id INT AUTO_INCREMENT PRIMARY KEY,
    word_id INT NOT NULL,
    next_word_id INT NOT NULL,
    follow_count INT DEFAULT 0,
    UNIQUE(word_id, next_word_id),
    FOREIGN KEY (word_id) REFERENCES Words(word_id),
    FOREIGN KEY (next_word_id) REFERENCES Words(word_id)
);

CREATE TABLE Generated_Sentences (
    sentence_id INT AUTO_INCREMENT PRIMARY KEY,
    sentence_text TEXT NOT NULL,
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

# 4. Optional DataGrip Setup

## Get DataGrip for Free (Students)

Sign up here:
https://www.jetbrains.com/community/education/

Download DataGrip after approval.

---

## Connect DataGrip to MySQL

1. Open DataGrip
2. Click **+ → Data Source → MySQL**
3. Enter:

   * Host: `localhost`
   * Port: `3306`
   * User: `root`
   * Password: your password
4. Click **Test Connection**
5. Click **Apply**

You should now see your `sentence_builder` database.

---

# 5. Java Project Setup

## Folder Structure

```text
SentenceBuilder/
├── src/
├── lib/
│   └── mysql-connector-j-9.x.x.jar
└── .vscode/
    └── settings.json
```

---

# 6. Install MySQL Connector (JDBC)

Download:
https://dev.mysql.com/downloads/connector/j/

Choose:

* Platform Independent (ZIP)

Extract and move the `.jar` file into:

```text
lib/
```

---

## Configure VS Code

Create `.vscode/settings.json`:

```json
{
  "java.project.referencedLibraries": [
    "lib/**/*.jar"
  ]
}
```

---

# 7. Configure Database Connection

Edit:

`src/util/DatabaseManager.java`

```java
private static final String URL = "jdbc:mysql://localhost:3306/sentence_builder";
private static final String USER = "root";
private static final String PASSWORD = "YOUR_PASSWORD";
```

---

# 8. Run the Project

## Compile and Run (Terminal)

```bash
cd src

javac -cp ".:../lib/mysql-connector-j-9.x.x.jar" Main.java util/DatabaseManager.java model/*.java dao/*.java service/*.java

java -cp ".:../lib/mysql-connector-j-9.x.x.jar" Main
```

---

## Run in VS Code

* Install Extension Pack for Java
* Right click `Main.java`
* Click **Run Java**

Do not use "Run Code".

---

# 9. Import a Gutenberg Book

Download a `.txt` file (example):

https://www.gutenberg.org/cache/epub/1342/pg1342.txt

Save it locally, then in the program:

* choose "Import text file"
* enter full file path

Example:

```text
/Users/yourname/Downloads/pg1342.txt
```

---

# Troubleshooting

## No suitable driver

* MySQL `.jar` not in `lib/`
* settings.json missing
* VS Code not restarted

---

## Access denied

* Wrong MySQL password

---

## Can't connect to MySQL

* MySQL server not running

---

## Tables not found

* CREATE TABLE commands not executed

---

# Summary

This project requires:

* Local MySQL database
* JDBC connector
* Java backend (DAO and services)
* Optional DataGrip for visualization

If something fails, check:

1. MySQL is running
2. Database exists
3. Connector `.jar` is loaded
4. Password is correct

---
