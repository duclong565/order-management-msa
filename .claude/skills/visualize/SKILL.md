---
name: visualize
description: "Add a correct, minimal visual to a lesson or explanation — a diagram or geometric picture — rendered inline in the reply. Use when an idea is genuinely clearer as a picture: a dependency graph, system/flow, sequence, state machine, tree, comparison, or a spatial/geometric thing (coordinate geometry, number line, vectors, a plot, a physical layout)."
---

# Visualize

> Adapted for Claude Code from [amosblomqvist/learn](https://github.com/amosblomqvist/learn) (originally written for the `pi` CLI, which dispatched a `mermaid-maker`/`svg-maker` subagent that rendered a PNG, visually inspected it, and embedded it into an Obsidian vault log). This environment has no such maker subagent or Obsidian log, so the invoke/embed steps below are rewritten to draw the diagram directly. The judgment calls — when to visualize, how to prune a brief — are unchanged.

A picture earns its place only when it shows something words can't — shape, structure, direction, relationship, geometry. This skill produces ONE such picture and drops it directly into the reply.

You are the **creative director** here — decide the exact idea and distill it to its fewest carrying elements — but in this environment you also author the diagram yourself. There is no separate maker subagent to inspect a render before you show it, so self-review carefully before presenting (see "Self-check before presenting" below).

## When to visualize (and when not to)

This teaching system builds a **dependency graph in the learner's head** — axioms at the root, derived facts hanging off them. A visual is powerful exactly when it makes that structure (or a geometry) visible. Reach for one when:

- The idea is a **structure or relationship**: dependencies, a system with parts and arrows, a flow/pipeline, a sequence of exchanges, a state machine, a tree/hierarchy, a comparison, a containment (what's inside vs outside).
- The idea is **spatial or geometric**: coordinate geometry, a number line, vectors, a function's shape, a physical arrangement.

Do NOT visualize when prose or a single equation already carries it. A decorative diagram that just restates the sentence next to it adds noise and a chance to be wrong. When in doubt, don't — a missing visual is cheaper than a false one.

## Choose the form

- **Structural/relational** (dependency graphs, flowcharts, sequence/state/ER/class diagrams, trees, mindmaps, timelines) — use a **Mermaid** code block. This is the default and fits the dependency-graph pedagogy directly. Mermaid's own layout engine means you describe nodes and edges and it handles placement — far less room for a visual bug than hand-placed coordinates.
- **Spatial/geometric** (exact coordinates, geometry figures, number lines, vectors, plots, custom shapes) — Mermaid can't lay these out. Author inline **SVG** instead, with an explicit `viewBox` and hand-placed coordinates.

Rule of thumb: if it's *nodes-and-edges / relationships*, Mermaid. If it's *positions-and-shapes / geometry*, SVG.

## Brief yourself well: one idea, fewest elements

The most common failure is **cramming** — every extra label makes the picture harder to read AND harder to lay out correctly. Before drafting, prune to the fewest elements that carry the idea, and for each ask: *"if I delete this, is the idea still clear?"* If yes, delete it.

Fix the concept AND the concrete elements before writing any diagram syntax — not a vague topic, and not a long checklist.

- BAD: "a diagram about how TCP works"
- GOOD: "graph TD: a node 'packet' at the top; arrows down to 'ordering' and 'retransmit on loss'; both arrows down into 'reliable stream'. No title. Show that reliability is built FROM packets, not alongside them."

If your own mental brief lists more than ~5–7 elements, cut it before you start drafting syntax.

## Render it

- **Mermaid**: write a fenced ` ```mermaid ` code block directly in your reply. This client renders Mermaid diagrams natively inline (both in plain chat replies and inside a published Artifact) — no external tool call needed.
- **SVG**: write the raw `<svg viewBox="...">...</svg>` directly in your reply, or use the `Artifact` tool if the picture is complex enough to be worth its own page (SVG artifacts should use CSS variables for theming — see the `artifact-diagramming` skill if this project has it enabled).

## Self-check before presenting (replaces the maker's render-and-inspect loop)

Since nothing here renders the diagram and looks at it for you before it reaches the user, do this manually:

1. **Re-read your own diagram source as data, not as intent.** For each node/edge you meant to include, check it's actually present in the syntax — a typo'd Mermaid arrow or a missing SVG coordinate silently drops or misplaces an element.
2. **Count elements against your brief.** If you briefed 4 nodes and 5 arrows, the source should have exactly that — not more (cramming crept back in) or fewer (something got dropped mid-edit).
3. **For SVG specifically**, sanity-check coordinates against the `viewBox` bounds — an element placed outside it renders invisibly, which is a silent failure you won't be warned about.
4. If you're not confident the diagram is correct after this check, prefer simplifying it (fewer elements are easier to verify) over shipping something you haven't actually verified — a missing visual is cheaper than a wrong one.

## Present it in the lesson

Introduce the visual in a sentence, then let it carry the idea — don't narrate every element back in prose afterward. Since it's inline in your reply (not a linked file), there's no separate embed step: the code block itself is the deliverable.
