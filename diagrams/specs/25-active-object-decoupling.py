"""Active Object: callers enqueue and return; one worker thread drains the queue and completes each future."""

from generate_diagram import emit

CX, CW, CH = 40, 170, 55                 # caller column
BX, BW = 310, 240                        # queue band
RX, RW, RH = 340, 180, 40                # request boxes inside the band
WX, WW = 650, 240                        # worker
RA, RB, RC = 150, 205, 260               # request rows
Y_CB = 88                                # return lane, above the band

emit(
    "25-active-object-decoupling",
    WX + WW + 40,
    460,
    bands=[
        {"x": BX, "y": 115, "w": BW, "h": 210, "fill": "#fafafa",
         "label": "requests: BlockingQueue<Runnable>"},
    ],
    nodes=[
        {"x": CX, "y": 60, "w": CW, "h": CH, "style": "user", "lines": ["Caller A", "submitPrint(requestA)"]},
        {"x": CX, "y": 145, "w": CW, "h": CH, "style": "user", "lines": ["Caller B", "submitPrint(requestB)"]},
        {"x": CX, "y": 230, "w": CW, "h": CH, "style": "user", "lines": ["Caller C", "submitPrint(requestC)"]},
        {"x": RX, "y": RA, "w": RW, "h": RH, "style": "sub", "lines": ["request A (queued)"]},
        {"x": RX, "y": RB, "w": RW, "h": RH, "style": "sub", "lines": ["request B (queued)"]},
        {"x": RX, "y": RC, "w": RW, "h": RH, "style": "sub", "lines": ["request C (queued)"]},
        {"x": WX, "y": 60, "w": WW, "h": 140, "style": "accent",
         "lines": ["label-print-worker", "one dedicated thread, drains FIFO"]},
        {"x": BX, "y": 350, "w": WX + WW - BX, "h": 55, "style": "ghost",
         "lines": ["Compare: ledger.print(request) blocks the caller",
                   "until the job finishes; nothing here decouples the two"]},
    ],
    edges=[
        {"x1": CX + CW, "y1": 112, "x2": BX, "y2": RA + RH // 2, "label": "enqueue", "lx": 12, "ly": -16},
        {"x1": CX + CW, "y1": 172, "x2": BX, "y2": RB + RH // 2},
        {"x1": CX + CW, "y1": 257, "x2": BX, "y2": RC + RH // 2},
        {"x1": BX + BW, "y1": RA + RH // 2, "x2": WX, "y2": RA + RH // 2, "label": "dequeue, FIFO",
         "lx": -8, "ly": -8},
        {"x1": WX, "y1": Y_CB, "x2": CX + CW, "y2": Y_CB, "amber": True, "dashed": True,
         "label": "future.complete(): later, on this thread", "lx": 0, "ly": -8},
    ],
    notes=[
        {"x": (WX + WW + 40) // 2, "y": 25, "anchor": "middle", "bold": True, "size": 13, "color": "#888",
         "text": "Active Object: the call returns before the work runs"},
        {"x": (WX + WW + 40) // 2, "y": 440, "anchor": "middle", "size": 11, "color": "#64748b",
         "text": "Every caller's CompletableFuture resolves off the caller's own thread: the one guarantee a direct call cannot make."},
    ],
)
