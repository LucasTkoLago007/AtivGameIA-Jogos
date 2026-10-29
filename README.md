# Atividade Game IA - Steering Behaviors

Atividade prática desenvolvida em Java com LibGDX.

## Controles

- `1`: Seek + Arrive
  - Clique com o mouse para escolher o alvo.
- `2`: Wander + FSM + Pursuit
  - O mouse representa o jogador.
  - Longe do jogador, o NPC vaga.
  - Ao entrar no raio de visão, o NPC passa a perseguir.
- `3`: Flocking (bônus)
  - 30 agentes usando separação, alinhamento e coesão.

## Vetores

- Verde: velocidade do agente.
- Magenta: força de steering.
- Círculo cinza: raio de visão no modo FSM.

## Executar no Windows

```powershell
.\gradlew.bat clean lwjgl3:run
```
