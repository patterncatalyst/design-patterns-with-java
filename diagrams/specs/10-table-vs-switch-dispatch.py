"""One (PACKED, SHIP) rule as an enum transition table and as a sealed-interface switch."""

from generate_diagram import emit

LP_X, LP_W = 20, 450               # left panel: enum + transition table
RP_X, RP_W = 610, 460              # right panel: sealed interface + switch
P_Y, P_H = 40, 420
HEAD_Y, HEAD_H = 70, 50

# Left: the states step right to left, so each arrow from apply() drops clear of the boxes above it.
ST_W, ST_H = 230, 70
CREATED = (220, 160)
PACKED = (130, 245)
IN_TRANSIT = (40, 330)

# Right: the case boxes are indented so ship() reaches case Packed past case Created.
CASE_X, CASE_W, CASE_H, CASE_Y, CASE_STEP = 670, 380, 34, 160, 42
CASES = [
    ("sub", "case Created c → reject(SHIP)"),
    ("accent", "case Packed p → new InTransit(trackingNumber)"),
    ("sub", "case InTransit t → reject(SHIP)"),
    ("sub", "case Delivered d → reject(SHIP)"),
    ("sub", "case Cancelled c → reject(SHIP)"),
    ("sub", "case Returned r → reject(SHIP)"),
    ("sub", "case Lost l → reject(SHIP)"),
]
PACKED_CASE_Y = CASE_Y + CASE_STEP


def state(pos, style, *lines):
    return {"x": pos[0], "y": pos[1], "w": ST_W, "h": ST_H, "style": style, "lines": list(lines)}


HEAD_BOTTOM = HEAD_Y + HEAD_H
W = RP_X + RP_W + 20

emit(
    "10-table-vs-switch-dispatch",
    W,
    510,
    bands=[
        {"x": LP_X, "y": P_Y, "w": LP_W, "h": P_H, "fill": "#fafafa", "label": "Enum + transition table"},
        {"x": RP_X, "y": P_Y, "w": RP_W, "h": P_H, "fill": "#fafafa", "label": "Sealed interface + switch"},
    ],
    nodes=[
        {"x": LP_X + 20, "y": HEAD_Y, "w": LP_W - 40, "h": HEAD_H, "style": "box",
         "lines": ["apply(ShipmentEvent event)", "one generic lookup for every status"]},
        state(CREATED, "box", "CREATED", "PACK → PACKED", "CANCEL → CANCELLED"),
        state(PACKED, "accent", "PACKED", "SHIP → IN_TRANSIT", "CANCEL → CANCELLED"),
        state(IN_TRANSIT, "box", "IN_TRANSIT", "DELIVER → DELIVERED", "RETURN_TO_SENDER → RETURNED"),
        {"x": RP_X + 20, "y": HEAD_Y, "w": RP_W - 40, "h": HEAD_H, "style": "box",
         "lines": ["ship(String carrierTrackingNumber)", "switch (state) { … }, no default"]},
        *[{"x": CASE_X, "y": CASE_Y + i * CASE_STEP, "w": CASE_W, "h": CASE_H, "style": s, "lines": [t]}
          for i, (s, t) in enumerate(CASES)],
    ],
    edges=[
        {"x1": CREATED[0] + 200, "y1": HEAD_BOTTOM, "x2": CREATED[0] + 200, "y2": CREATED[1]},
        {"x1": PACKED[0] + 30, "y1": HEAD_BOTTOM, "x2": PACKED[0] + 30, "y2": PACKED[1], "amber": True,
         "label": "status=PACKED, event=SHIP", "lx": 85, "ly": -37.5},
        {"x1": IN_TRANSIT[0] + 30, "y1": HEAD_BOTTOM, "x2": IN_TRANSIT[0] + 30, "y2": IN_TRANSIT[1]},
        {"x1": RP_X + 30, "y1": HEAD_BOTTOM, "x2": CASE_X, "y2": PACKED_CASE_Y + 13, "amber": True,
         "label": "ship(trackingNumber)", "lx": 60, "ly": -21.5},
        {"x1": PACKED[0] + ST_W, "y1": PACKED[1] + 17, "x2": CASE_X, "y2": PACKED_CASE_Y + 26,
         "amber": True, "dashed": True, "label": "same rule, two idioms", "lx": 25, "ly": -16},
    ],
    notes=[
        {"x": W // 2, "y": 25, "anchor": "middle", "bold": True, "size": 13, "color": "#888",
         "text": "The identical (PACKED, SHIP) → IN_TRANSIT rule, stated two ways"},
        {"x": LP_X + LP_W // 2, "y": 430, "anchor": "middle", "size": 10.5, "color": "#888",
         "text": "DELIVERED, CANCELLED, RETURNED, LOST: absent, no entry to look up."},
        {"x": W // 2, "y": 492, "anchor": "middle", "size": 11.5, "color": "#64748b",
         "text": "Table: 3 reachable statuses only. Switch: all 7, compiler-enforced: a forgotten case fails the build."},
    ],
)
