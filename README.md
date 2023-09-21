# Java Chess with Minimax AI Bot

Welcome to the Java Chess project with a Minimax AI bot! This project allows you to play chess against a computer opponent that uses the Minimax algorithm to make strategic decisions. You can test your chess skills and challenge yourself against this intelligent opponent.

![Chess](https://betacssjs.chesscomfiles.com/bundles/web/images/offline-play/standardboard.png)

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [How to Play](#how-to-play)
- [Minimax Algorithm](#minimax-algorithm)
- [AI Bot](#ai-bot)
- [Contributing](#contributing)
- [License](#license)

## Prerequisites
Before you get started, make sure you have the following prerequisites installed:

- Java Development Kit (JDK) 8 or higher
- Git (optional, for cloning the repository)

## Installation
1. Clone this repository (if you haven't already):

   ```bash
   git clone https://github.com/Aayush-Joshi-01/Chess.git
   ```

2. Compile the Java source files:

   ```bash
   javac src/*.java
   ```

## How to Play
1. Run the game:

   ```bash
   java -cp src Main
   ```

2. The game will display the chessboard and prompt you to enter moves in the format `source_square destination_square`. For example, to move a pawn from e2 to e4, you would enter `e2 e4`.

3. Play your moves, and the AI bot will respond with its moves based on the Minimax algorithm.

4. Keep playing until there is a checkmate, stalemate, or you decide to quit.

## Minimax Algorithm
The Minimax algorithm is a decision-making algorithm used in two-player games like chess. It is a recursive algorithm that evaluates possible moves to find the best move for the current player while assuming the opponent will make the worst move possible. The algorithm explores the game tree by recursively analyzing possible moves and their outcomes, and it assigns a score to each possible move.

Here's a high-level overview of how the Minimax algorithm works:

1. The AI bot considers all possible moves it can make.
2. For each possible move, it simulates the game to a certain depth (controlled by the `depth` parameter) by making hypothetical moves for both players.
3. It evaluates the resulting game state and assigns a score to that move.
4. If the AI is the maximizing player, it chooses the move with the highest score (best for itself).
5. If the AI is the minimizing player, it chooses the move with the lowest score (worst for the opponent).

By recursively applying this process, the AI bot can determine the best move to make based on the current game state and the predicted future game states.

You can customize the AI's difficulty by adjusting the depth of the Minimax algorithm in the `AIPlayer.java` file.

```java
// Set the depth for the Minimax algorithm
int depth = 3; // Change this value to increase/decrease difficulty
```

## AI Bot
The AI bot in this chess game uses the Minimax algorithm to make decisions, as explained above. It evaluates possible moves, predicts future game states, and selects the best move accordingly. Challenge yourself by playing against this intelligent opponent!

## Contributing
If you'd like to contribute to this project, feel free to open issues, submit pull requests, or suggest improvements. We welcome your ideas and contributions to make this chess game even better!

## License
This Java Chess project is licensed under the [MIT License](LICENSE). You are free to use, modify, and distribute this software as long as you include the original copyright notice and disclaimer.

---

Enjoy playing chess against the Minimax AI bot! If you have any questions or encounter any issues, please don't hesitate to reach out or open an issue on the GitHub repository. Happy gaming!
