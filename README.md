# Cricket Analytics Engine (Java)

This project parses Cricsheet JSON match files and generates:

- Batter vs Bowler-Type statistics
- Bowler vs LHB/RHB statistics
- Wide-format simulation sheet output

## Tech Stack
- Java
- Maven
- Jackson
- OpenCSV

## How To Run

1. Place match JSON files inside `/matches`
2. Add player roles in `playerRoles.csv`
3. Run:

mvn clean compile exec:java
