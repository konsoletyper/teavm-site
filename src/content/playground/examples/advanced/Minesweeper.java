/*
 *  Copyright 2025 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import java.util.ArrayDeque;
import java.util.Random;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

private static final HTMLDocument document = HTMLDocument.current();
private static final int FIELD_SIZE = 12;
private static final float MINE_RATIO = 1 / 7f;
private static Cell[][] field;
private static int remainingCells;
private static boolean gameOver;

void main() {
    initField();
    placeMines();
    initStyle();
    initUI();
}

void initField() {
    field = new Cell[FIELD_SIZE][FIELD_SIZE];
    remainingCells = FIELD_SIZE * FIELD_SIZE;
    for (var i = 0; i < FIELD_SIZE; ++i) {
        for (var j = 0; j < FIELD_SIZE; ++j) {
            field[i][j] = new Cell();
        }
    }
}

void placeMines() {
    var mineCount = (int) (remainingCells * MINE_RATIO);
    var random = new Random();
    for (var i = 0; i < mineCount; ++i) {
        int row;
        int column;
        do {
            row = random.nextInt(FIELD_SIZE);
            column = random.nextInt(FIELD_SIZE);
        } while (field[row][column].mine);
        field[row][column].mine = true;
    }
    remainingCells -= mineCount;
}

void initStyle() {
    var styleElem = document.createElement("style");
    styleElem.setTextContent("""
        html, body {
            height: 100%;
            padding: 0;
            margin: 0;
        }
        body {
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
        }
        .field {
            display: grid;
            grid-template-columns: repeat(FIELD_SIZE, 1fr);
            grid-template-rows: repeat(FIELD_SIZE, 1fr);
            container-type: inline-size;
            font-weight: bold;
            font-family: sans-serif;
            width: 100vmin;
            height: 100vmin;
        }
        .cell {
            border-right: solid 1px black;
            border-bottom: solid 1px black;
            background-position: 10% 10%;
            background-size: 90%;
            display: flex;
            justify-content: center;
            align-items: center;
        }
        .cell.closed:hover {
            background-color: #0000000f;
        }
        .cell.open {
            background-color: #0000002f;
        }
        .cell.open > span {
            font-size: 4cqh;
            display: inline-block;
        }
        .mine {
            background-image: url("data:image/svg+xml;utf8,MINE");
            background-repeat: no-repeat;
        }
        .flag {
            background-image: url("data:image/svg+xml;utf8,FLAG");
            background-repeat: no-repeat;
        }
    """.replace("FIELD_SIZE", String.valueOf(FIELD_SIZE))
            .replace("MINE", MINE_IMAGE.replace("\"", "\\\""))
            .replace("FLAG", FLAG_IMAGE.replace("\"", "\\\"")));
    document.getHead().appendChild(styleElem);
}

void initUI() {
    var container = document.createElement("div");
    document.getBody().appendChild(container);
    container.setClassName("field");

    for (var i = 0; i < FIELD_SIZE; ++i) {
        for (var j = 0; j < FIELD_SIZE; ++j) {
            var cell = field[i][j];
            var element = document.createElement("div");
            element.getClassList().add("cell", "closed");
            cell.element = element;
            container.appendChild(element);

            var row = i;
            var column = j;
            cell.element.onEvent("contextmenu", event -> {
                toggleFlag(row, column);
                event.preventDefault();
            });
            cell.element.onClick(event -> {
                if (event.getButton() == MouseEvent.LEFT_BUTTON) {
                    openCell(row, column);
                }
            });
        }
    }
}

void openCell(int row, int column) {
    if (gameOver) {
        return;
    }
    var cell = field[row][column];
    if (cell.open || cell.flag) {
        return;
    }
    if (cell.mine) {
        loseGame();
        return;
    }

    var queue = new ArrayDeque<Integer>();
    queue.addLast(row);
    queue.addLast(column);
    while (!queue.isEmpty()) {
        row = queue.removeFirst();
        column = queue.removeFirst();
        cell = field[row][column];
        if (cell.open || cell.mine) {
            continue;
        }
        cell.open = true;
        cell.element.getClassList().remove("closed");
        cell.element.getClassList().add("open");
        if (--remainingCells == 0) {
            gameOver = true;
        }
        var mines = minesAround(row, column);
        if (mines > 0) {
            var span = document.createElement("span");
            span.setInnerText(String.valueOf(mines));
            cell.element.appendChild(span);
        } else {
            if (row > 0) {
                if (column > 0) {
                    queue.addLast(row - 1);
                    queue.addLast(column - 1);
                }
                queue.addLast(row - 1);
                queue.addLast(column);
                if (column < FIELD_SIZE - 1) {
                    queue.addLast(row - 1);
                    queue.addLast(column + 1);
                }
            }
            if (row < FIELD_SIZE - 1) {
                if (column > 0) {
                    queue.addLast(row + 1);
                    queue.addLast(column - 1);
                }
                queue.addLast(row + 1);
                queue.addLast(column);
                if (column < FIELD_SIZE - 1) {
                    queue.addLast(row + 1);
                    queue.addLast(column + 1);
                }
            }
            if (column > 0) {
                queue.addLast(row);
                queue.addLast(column - 1);
            }
            if (column < FIELD_SIZE - 1) {
                queue.addLast(row);
                queue.addLast(column + 1);
            }
        }
    }
}

int minesAround(int row, int column) {
    var count = 0;
    if (row > 0) {
        if (column > 0 && field[row - 1][column - 1].mine) {
            ++count;
        }
        if (field[row - 1][column].mine) {
            ++count;
        }
        if (column < FIELD_SIZE - 1 && field[row - 1][column + 1].mine) {
            ++count;
        }
    }
    if (row < FIELD_SIZE - 1) {
        if (column > 0 && field[row + 1][column - 1].mine) {
            ++count;
        }
        if (field[row + 1][column].mine) {
            ++count;
        }
        if (column < FIELD_SIZE - 1 && field[row + 1][column + 1].mine) {
            ++count;
        }
    }
    if (column > 0 && field[row][column - 1].mine) {
        ++count;
    }
    if (column < FIELD_SIZE - 1 && field[row][column + 1].mine) {
        ++count;
    }
    return count;
}

void toggleFlag(int row, int column) {
    if (gameOver) {
        return;
    }
    var cell = field[row][column];
    if (cell.open) {
        return;
    }
    cell.flag = !cell.flag;
    if (!cell.flag) {
        cell.element.getClassList().remove("flag");
    } else {
        cell.element.getClassList().add("flag");
    }
}

void loseGame() {
    gameOver = true;
    for (var i = 0; i < FIELD_SIZE; ++i) {
        for (var j = 0; j < FIELD_SIZE; ++j) {
            var cell = field[i][j];
            if (cell.mine) {
                cell.element.getClassList().add("mine");
            }
        }
    }
}

static class Cell {
    boolean mine;
    boolean flag;
    boolean open;
    HTMLElement element;
}

static final String MINE_IMAGE = "<svg xmlns=\"http://www.w3.org/2000/svg\" "
        + "height=\"24px\" viewBox=\"0 -960 960 960\" width=\"24px\">"
        + "<path d=\"M346-48q-125 0-212.5-88.5T46-350q0-125 86.5-211.5T344-648h13l27-47q12-22 36-28.5t46 6.5l30 "
        + "17 5-8q23-43 72-56t92 12l35 20-40 69-35-20q-14-8-30.5-3.5T570-668l-5 8 40 23q21 12 27.5 36t-5.5 "
        + "45l-27 48q23 36 34.5 76.5T646-348q0 125-87.5 212.5T346-48Zm0-80q91 0 "
        + "155.5-64.5T566-348q0-31-8.5-61T532-466l-26-41 42-72-104-60-42 72h-44q-94 0-163.5 60T125-350q0 92 64.5 "
        + "157T346-128Zm454-480v-80h120v80H800ZM580-828v-120h80v120h-80Zm195 81-56-56 85-85 56 56-85 "
        + "85ZM346-348Z\"/></svg>";

static final String FLAG_IMAGE = "<svg xmlns=\"http://www.w3.org/2000/svg\" height=\"24px\" "
        + "viewBox=\"0 -960 960 960\" width=\"24px\"><path d=\"M200-120v-680h360l16 "
        + "80h224v400H520l-16-80H280v280h-80Zm300-440Zm86 160h134v-240H510l-16-80H280v240h290l16 80Z\"/></svg>";
