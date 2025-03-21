# OLA3-db - Mandatory Assignment 3

### Timothy Busk Mortensen - cph-tm246@cphbusiness.dk

---

- First, setup the databse in mySQL. Then run the script(mySQl cmd line or in workbench)
- Second, create a small java project. 

---

# Optimistic & Pessimistic Concurrency Control

## 1. Implement Optimistic Concurrency Control for Tournament Updates

`Task:`
- 1. Add a version column to Tournaments.
- 2. Implement version-based optimistic concurrency control
- 3. Ensure that only one change is successful when two concurrent admins try to update.

`ALTER TABLE Tournaments ADD COLUMN version INT NOT NULL DEFAULT 1;`
