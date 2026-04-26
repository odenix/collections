## Repo Instructions

- Before doing port work, read `docs/porting.md`, `docs/porting-implementation.md`, and `docs/porting-deviations.md`.
- Treat the directory containing this `AGENTS.md` file as the repo root.
- Put temporary files and scratch directories under `.agents/`, not in the repo root.

- For Mill, use only the repo-relative `.agents/out` directory as the Mill output directory.
- In this repo, run Mill with `--no-daemon`.
- Initialize the upstream source checkout with `MILL_OUTPUT_DIR=.agents/out ./mill --no-daemon initUpstream` on Unix-like shells or `$env:MILL_OUTPUT_DIR='.agents/out'; ./mill.bat --no-daemon initUpstream` in PowerShell.
- Run Mill from the repo root with task names: `compile`, `build`, `test.compile`, `test`, `test.testOnly ...`, `test.extra.compile`, `test.extra`, `test.extra.testOnly ...`, or `test.extra.randomized`.
- From `cmd.exe`, invoke Mill as `cd /d <repo-root> && set "MILL_OUTPUT_DIR=.agents/out" && mill.bat --no-daemon test`.
- In PowerShell, prefer the direct repo-root script form, for example: `Set-Location <repo-root>; $env:MILL_OUTPUT_DIR='.agents/out'; ./mill.bat --no-daemon test`.
- In this Codex PowerShell tool environment, `workdir` may not leave the shell at the repo root, so explicitly `Set-Location <repo-root>` before invoking `./mill.bat`.
- Do not create or switch to any other agent-specific Mill output directory.
- Run one Mill command at a time, avoid exploratory startup commands such as `--help`, and do not immediately retry after an interrupted run without checking for stale lock state in `.agents/out`.
- If `.agents/out` causes problems, ask the user for help instead of inventing a new output directory.
- The root task path is correct even if a stale `.agents/out/mill-daemon/launcherLock` blocks startup.
