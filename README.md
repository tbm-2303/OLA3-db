# OLA3-db - Mandatory Assignment 3

### Timothy Busk Mortensen - cph-tm246@cphbusiness.dk

---

# Optimistic & Pessimistic Concurrency Control

## 1. Implement Optimistic Concurrency Control for Tournament Updates
Task:
- `Add a version column to Tournaments.`
- `Implement version-based optimistic concurrency control.`
- `Ensure that only one change is successful when two concurrent admins try to update.`

### Problem: Two admins attempt to change the start date for the same tournament at the same time.

Uden concurrency control ville først den ene update køre, hvorefter den anden update ville overskrive dataet fra den forhåndværende update. Optimistisk samtidigheds kontrol virker ved kun at acceptere den første update. Den anden vil ikke blive accepteret og admin vil således blive informeret.  

- ALTER TABLE Tournaments ADD COLUMN version INT NOT NULL DEFAULT 1;

- Jeg har benyttet samme fremgangmåde som den vedlagte note: [Optimistic Concurrency Control](https://github.com/Tine-m/final-assignment/blob/main/application-concurrency-note.md#how-optimistic-concurrency-control-works). For at vise concurrency problematikken har jeg valgt at oprette 2 threads. Dette skal simulere 2 admins, som begge forsøger at updatere den samme turnering på samme tid. Derfor er der også en lille indlagt pause `sleep(2000)` for at sikre at begge tråde har læst dataet inden opdatering finder sted.

- Kun den ene tråd blev accepteret. Den anden blev, som forvented, afvist. 
![text](OptimisticConcurrency.png) 