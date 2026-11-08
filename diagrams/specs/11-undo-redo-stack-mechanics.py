"""CommandHistory moves commands between two Deques: undo() shifts the top, execute() clears undone."""

from generate_diagram import emit

PW, GAP, X0 = 290, 30, 20          # panel width, gap, first panel x
PX = [X0 + i * (PW + GAP) for i in range(3)]
UND, DONE = 20, 165                # column offsets inside a panel (undone left, done right)
BW, BH = 80, 50
R1, R2, R3 = 190, 260, 330         # stack rows; the top of every stack sits highest
Y_TOP, Y_PH = 50, 380              # panel top and height


def box(panel, col, row, style, *lines):
    return {"x": PX[panel] + col, "y": row, "w": BW, "h": BH, "style": style, "lines": list(lines)}


def header(panel, col, text):
    return {"x": PX[panel] + col + BW // 2, "y": 110, "anchor": "middle", "bold": True,
            "size": 12, "color": "#888", "text": text}


W = PX[2] + PW + 20
MID = W // 2

# undo() edge: panel 1 done C3 -> panel 2 undone C3, label sits above the panel 1 top row
E1_X1, E1_X2, E1_Y = PX[0] + DONE + BW, PX[1] + UND, R1 + BH // 2
# execute(C4) edge: panel 2 undone C3 -> panel 3 cleared, label over the empty done slot
E2_X1, E2_X2 = PX[1] + UND + BW, PX[2] + UND

emit(
    "11-undo-redo-stack-mechanics",
    W,
    500,
    bands=[
        {"x": PX[0], "y": Y_TOP, "w": PW, "h": Y_PH, "fill": "#fafafa",
         "label": "1. execute(C1) → execute(C2) → execute(C3)"},
        {"x": PX[1], "y": Y_TOP, "w": PW, "h": Y_PH, "fill": "#fafafa", "label": "2. undo()"},
        {"x": PX[2], "y": Y_TOP, "w": PW, "h": Y_PH, "fill": "#fafafa",
         "label": "3. execute(C4): a new action"},
    ],
    nodes=[
        box(0, DONE, R1, "accent", "C3", "(top)"),
        box(0, DONE, R2, "box", "C2"),
        box(0, DONE, R3, "box", "C1"),
        box(0, UND, R1, "ghost", "empty"),
        box(1, DONE, R2, "accent", "C2", "(top)"),
        box(1, DONE, R3, "box", "C1"),
        box(1, UND, R1, "accent", "C3"),
        box(2, DONE, R1, "accent", "C4", "(top)"),
        box(2, DONE, R2, "box", "C2"),
        box(2, DONE, R3, "box", "C1"),
        box(2, UND, R1, "ghost", "cleared"),
    ],
    edges=[
        {"x1": E1_X1, "y1": E1_Y, "x2": E1_X2, "y2": E1_Y, "amber": True,
         "label": "undo(): done.poll() → C3.undo() → undone.push(C3)",
         "lx": PX[0] + PW // 2 - (E1_X1 + E1_X2) / 2, "ly": -(BH // 2) - 12},
        {"x1": E2_X1, "y1": E1_Y, "x2": E2_X2, "y2": E1_Y, "amber": True, "dashed": True,
         "label": "execute(C4): undone.clear()",
         "lx": PX[1] + DONE + BW // 2 - (E2_X1 + E2_X2) / 2, "ly": -8},
    ],
    notes=[
        {"x": MID, "y": 25, "anchor": "middle", "bold": True, "size": 13, "color": "#888",
         "text": "CommandHistory: two Deques, done and undone, moved as a stack"},
        *[header(p, c, t) for p in range(3) for c, t in ((DONE, "done"), (UND, "undone"))],
        {"x": MID, "y": 470, "anchor": "middle", "size": 11, "color": "#64748b",
         "text": "Running a new command always clears undone, so redo() never contradicts what just happened."},
    ],
)
