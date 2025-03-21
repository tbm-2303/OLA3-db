# OLA3-db - Mandatory Assignment 3

### Timothy Busk Mortensen - cph-tm246@cphbusiness.dk

---

# Optimistic & Pessimistic Concurrency Control

## 1. Implement Optimistic Concurrency Control for Tournament Updates

Task:
- `Add a version column to Tournaments.`
- `Implement version-based optimistic concurrency control.`
- `Ensure that only one change is successful when two concurrent admins try to update.`



- ALTER TABLE Tournaments ADD COLUMN version INT NOT NULL DEFAULT 1;

- Jeg har benyttet samme fremgangmåde som den vedlagte note: [Optimistic Concurrency Control](https://github.com/Tine-m/final-assignment/blob/main/application-concurrency-note.md#how-optimistic-concurrency-control-works). For at vise concurrency problematikken har jeg valgt at oprette 2 threads. Dette skal simulere 2 admins, som begge forsøger at updatere den samme turnering på samme tid. Derfor er der en lille indlagt pause `sleep(2000)` for at sikre at begge tråde har læst dataet inden en begynder på opdatering. 

- Kun den ene tråd var i stand til at 
![text](OptimisticConcurrency.png) 