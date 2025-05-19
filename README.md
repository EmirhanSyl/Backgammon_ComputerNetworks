# Backgammon — Online Edition

### Concise Technical Report
![image](https://github.com/user-attachments/assets/8bbc1661-05e8-4bdb-a55c-0a1892821d2c)


---

## 1  Functional Capabilities

| Feature              | Short description                                                                                                                                                                                      |
| -------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **Checker movement** | Click source → click target. All rule-checking is performed on the server.                                                                                                                             |
| **Turn management**  | Each physical die can be used once (or twice when doubled); when `diceUsed` is full the server rolls for the next player.                                                                              |
| **Bar entry**        | A checker on the bar must re-enter into *opponent’s* home board (`die` ⇒ WHITE = point 1-6, BLACK = 24-19).                                                                                            |
| **Blot hitting**     | Landing on an enemy point that contains exactly one opposing checker puts that checker on the bar.                                                                                                     |
| **Automatic pass**   | If *no* re-entry or board move is legal, the server immediately passes the turn and rolls.                                                                                                             |
| **Bearing-off**      | When all fifteen checkers are inside the player’s home board, a move of `to = 25` removes a checker. The UI shows a wide “OFF” column on the right; clicking anywhere inside it performs the bear-off. |
| **Game over**        | The first player to bear-off 15 checkers triggers `GAME_OVER`; the room is reset automatically.                                                                                                        |

---

## 2  Code Architecture & Design Patterns

| Pattern                  | Where / why?                                                                                                                                                                       |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **MVC-like split**       | `BoardPanel` (view) ←→ `OnlineMoveController` / `MovePhaseController` (controller) ←→ `GameState` (model).                                                                         |
| **State pattern**        | `GameContext` holds a current `GameStateController` implementation (`OpeningRollController`, `MovePhaseController`, `OnlineMoveController`) and switches states without if-chains. |
| **Strategy / Validator** | `MoveValidator` interface → `StandardMoveValidator` encapsulates *all* legal-move logic.  Rules can be swapped (e.g., Narde, HyperGammon).                                         |
| **Adapter**              | `GameStateControllerAdapter` provides default empty methods so an online controller overrides only `onPointSelected`.                                                              |
| **Observer (push)**      | `ClientNetwork` pushes each `LegacyMessage` to a consumer that is hot-swapped from `WaitingScreen` to `GameScreen`.                                                                |

---

## 3  Network Layer

### 3.1 Server

* **`BackgammonServer`** – single-room game loop, keeps authoritative `GameState`.
* **`ClientHandler`** – one per TCP socket, converts raw lines to `LegacyMessage`, validates moves, broadcasts state.
* **`StandardMoveValidator`** – reused on the server to stop cheating.

### 3.2 Client

* **`ClientNetwork`** – background listener thread; every received line is decoded and forwarded to the EDT consumer.
* **`WaitingScreen` → `GameScreen`** – GUI stages; consumer pointer is switched via `setConsumer`.
* **`OnlineMoveController`** – constructs only `MOVE` packets; never mutates local state directly—keeps one source of truth.

### 3.3 Patterns

| Pattern      | Application                                                  |
| ------------ | ------------------------------------------------------------ |
| **Adapter**  | Runtime swap of network message consumer.                    |
| **Observer** | Asynchronous, push-style message delivery from socket to UI. |

---

## 4  Legacy TCP Message Protocol

Single-line, pipe-separated, key-value format:

```
TYPE | key1=value1 | key2=value2 | ...
```

| Type           | Sent by → To           | When / why                       | Minimal payload |
| -------------- | ---------------------- | -------------------------------- | --------------- |
| `HELLO`        | Client → Server        | Immediately after socket open    | nickname        |
| `WELCOME`      | Server → Client        | On colour assignment             | playerColor     |
| `START`        | Server → Both          | Start of each game/set           | state, dice     |
| `MOVE`         | Active Client → Server | On each user move                | from, to, die   |
| `ILLEGAL_MOVE` | Server → Client        | Rule violation                   | reason          |
| `STATE_UPDATE` | Server → Both          | After every legal move (or pass) | state, dice     |
| `GAME_OVER`    | Server → Both          | 15 checkers borne off            | winnerColor     |
| `ERROR`        | Either side            | Protocol / room errors           | message         |
| `PING / PONG`  | Either side            | Keep-alive (optional)            | —               |

#### Typical examples

```text
MOVE|from=12|to=16|die=4
STATE_UPDATE|state=1:W2;...;diceUsed=10;turn=BLACK|dice=3,5
GAME_OVER|winnerColor=WHITE
```

* The **server** is the single authority: only it issues `STATE_UPDATE`.
* Clients rebuild their view exclusively from the last snapshot; no local prediction.

---

## 5  Data-flow Overview

See the accompanying **Mermaid sequence diagram** for an end-to-end view:

1. **Handshake** & colour assignment
2. First `START` with full board snapshot
3. *MOVE* from active client → server validation → `STATE_UPDATE` to both clients
4. Automatic pass if bar entry & board moves are blocked
5. Optional `PING / PONG`
6. `GAME_OVER` and room reset

---

## 6  Key Classes (quick reference)

| Layer      | Class                   | Responsibility                                           |
| ---------- | ----------------------- | -------------------------------------------------------- |
| Model      | `GameState`             | Board, dice, bar/off counters, turn, helper mutators     |
| Model      | `StandardMoveValidator` | Pure rule engine (*isLegal*, *playerHasMove*)            |
| Controller | `OnlineMoveController`  | Client-side UX, builds `MOVE` messages                   |
| Controller | `MovePhaseController`   | Offline/single-seat play (still reused for highlighting) |
| Network    | `ClientNetwork`         | TCP I/O, consumer swap (`setConsumer`)                   |
| Network    | `LegacyMessage`         | Dead-simple encoder/decoder, no external JSON lib        |
| Server     | `BackgammonServer`      | Game loop, pass detection, broadcasts                    |
| Server     | `ClientHandler`         | Per-socket thread; validates, relays                     |

---

![0e9d0e94-5f7e-484b-aae4-b8f43fc641ac (1)](https://github.com/user-attachments/assets/68ffa892-2772-4847-b979-e3269b381d94)

Reading the Diagram
| Phase          | Message(s)                  | Direction                             | Purpose                                             |
| -------------- | --------------------------- | ------------------------------------- | --------------------------------------------------- |
| **Handshake**  | `HELLO`, `WELCOME`          | Client → Server, Server → Client      | Register nickname and assign colour.                |
| **Start**      | `START`                     | Server → Both                         | Full board snapshot, dice and whose turn.           |
| **Move**       | `MOVE`, `STATE_UPDATE`      | Active client → Server, Server → Both | One legal checker move, new global state broadcast. |
| **Pass**       | *(implicit)* `STATE_UPDATE` | Server → Both                         | Sent when current player has **no legal moves**.    |
| **Keep-alive** | `PING`, `PONG`              | Either way                            | Optional TCP health check.                          |
| **Error**      | `ILLEGAL_MOVE`, `ERROR`     | Server → Client                       | Rule violation or protocol error.                   |
| **Game end**   | `GAME_OVER`                 | Server → Both                         | All 15 checkers borne off; room immediately resets. |

The bear-off action is just another MOVE whose to field is 25;
OnlineMoveController highlights the wide “OFF” column so the user can click anywhere inside to collect a checker.

