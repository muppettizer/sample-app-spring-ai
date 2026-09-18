# Docker Sandbox
https://www.docker.com/products/docker-sandboxes/

## Setup
```bash
brew install docker/tap/sbx # mac
winget install Docker.sbx # windows
sbx login
```

## Why?
ive agents the autonomy they need to get work done, safely.
It's primarily aimed at running AI coding agents (Claude Code, Codex, Gemini CLI, etc.) safely

Docker Sandboxes run AI coding agents in isolated microVM sandboxes. 
Each sandbox gets its own Docker daemon, filesystem, and network — the agent can build containers, install packages, and modify files without touching your host system.

Sandboxes let them run fast without running wild, so speed and safety stop being a tradeoff.
Agent containers created from the -docker templates run in privileged mode inside the microVM (not on your host).


All sandbox templates are Ubuntu & include Git, Docker CLI, and common development tools like Node.js, Python, Go, and Java.
Most variants include Git, Docker CLI, and common development tools like Node.js, Python, Go, and Java.

Use containers when you need lightweight packaging without Docker access. 
Use sandboxes when you need to give something autonomous full Docker capabilities without trusting it with your host environment.

sbx is specifically for CLI-based coding agents that run autonomously — it's not relevant to Copilot Chat in IntelliJ at all.
sbx run copilot → only worth adopting if you want to use Copilot in autonomous/agent mode where it takes multi-step actions on your codebase without you approving each one.
It becomes valuable when you start saying "Copilot, implement this feature end to end" and walking away.

Filesystem + Network + Credentials

## Start

```bash
sbx run copilot --template docker/sandbox-templates:copilot ~/GitHub/sample-app-spring-ai

sbx run copilot -- --yolo -p "review this PR"

echo "$(gh auth token)" | sbx secret set -g github
```

## Lifecycle
sbx run initializes a VM with a workspace for a specified agent and starts the agent. You can stop and restart without recreating the VM, preserving installed packages and Docker images.
Sandboxes persist until explicitly removed.

## Customise
https://docs.docker.com/ai/sandboxes/customize/

Templates vs Kits — which to use?
Templates = bake tools/certs into a Docker image once, reuse it everywhere. Best when the same environment is needed across your whole team.
Kits = declarative YAML that runs setup steps at sandbox creation. More portable (just a folder), easier to update without rebuilding an image.

### Templates
Installing the ZScaler CA Cert via a Template
See `Dockerfile.sbx` & docker-compose.sbx.yml
```bash
docker compose -f docker-compose.sbx.yml build
# Push to your Artifactory repo to share SBX template with team
docker login artifactory.mycompany.com
docker tag my-org/copilot-zscaler:v1 artifactory.mycompany.com/docker-local/copilot-zscaler:v1
docker push artifactory.mycompany.com/docker-local/copilot-zscaler:v1
# Then teammates just run:
sbx run --template artifactory.mycompany.com/docker-local/copilot-zscaler:v1 copilot
```

### Kits
Installing the ZScaler CA Cert via a Kit
The right way to handle your ZScaler cert is with a mixin kit. A kit packages a set of capabilities a sandbox can use, such as tools to install, environment variables to set, credentials to inject, domains to allow, files to drop in, and startup commands to run. 
```bash
zscaler-kit/
├── spec.yaml
└── files/
    └── home/
        └── .local/
            └── share/
                └── ca
```

To run Copilot with the kit for the project
```bash
cd ~/sample-app-spring-ai
sbx run copilot --kit ./zscaler-kit/
```
Without extra args, the sandbox runs copilot `--yolo` by default. 
Args after `--` replace these defaults, so to keep `--yolo`
```bash
sbx run copilot --kit /path/to/zscaler-kit/ -- --yolo
```
The apps docker-compose.yml just works inside.


## Debugging tip
If you hit SSL errors, check what the proxy intercepted:
```bash
sbx policy log

# And to inspect the cert was installed correctly:
sbx exec <sandbox-name> -- update-ca-certificates --verbose
```


# Workflow
Your IntelliJ stays exactly as-is for editing code. The sandbox is where Copilot runs and where your app stack lives — it mounts your project folder from the host, so edits you make in IntelliJ are instantly visible inside the sandbox.

1. Start your sandbox from your project root
```bash
cd ~/sample-app-spring-ai
sbx run --template artifactory.mycompany.com/docker-local/copilot-zscaler:v1 copilot
```

2. Inside the sandbox, start your app stack with Docker Compose:
```bash
docker compose up -d
```

3. Edit in IntelliJ as normal
Since your project folder is mounted into the sandbox, any file you save in IntelliJ is immediately reflected inside the sandbox — no sync needed. Copilot sees your latest code, your running containers pick up changes if you have hot reload configured (Spring Boot DevTools, Angular ng serve).

4. Let Copilot work
Copilot operates in --yolo mode inside the isolated microVM, so it can freely edit files, run builds, restart services — without touching anything outside your project folder on your host.

## Typical Iteration Loop
```bash
Edit in IntelliJ
      ↓
File saved to host disk
      ↓
Sandbox sees change instantly (shared mount)
      ↓
Spring DevTools reloads / Angular watcher rebuilds
      ↓
Test in browser / Copilot picks up the change
      ↓
Ask Copilot to fix a bug or extend a feature
      ↓
Copilot edits files → IntelliJ detects external change → reload
```

# End-to-end example
```bash
## Store your GitHub token so Copilot and gh CLI can auth
echo "$(gh auth token)" | sbx secret set -g github

## Start the sandbox on your project
cd ~/sample-app-spring-ai
sbx run --template artifactory.mycompany.com/docker-local/copilot-zscaler:v1 copilot

## Tell Copilot what to do a feature
> Create a new Git branch called feature/user-profile-endpoint, then implement 
  a GET /api/users/{id}/profile endpoint in the Spring Boot app that returns 
  the user's name, email and createdAt date. Add a service class, repository 
  method, and a basic unit test. Commit the changes with a meaningful message.
  
## Option A — Push branch and raise a PR
> Push the branch to origin and create a pull request to main with a 
  description of what was implemented.
  
## Option B — Just push the branch, review locally in IntelliJ
> Push the branch to origin.

```

## Clone mode
Clone mode + Claude Code's agents view — what the docs described
Claude Code has (or had) an agents view — a UI within Claude Code itself that lets a single Claude Code instance act as an orchestrator, spawning multiple sub-agents to work on tasks in parallel.
Clone mode is the infrastructure that makes that safe:

With clone mode (--clone), the sandbox contains a private Git clone. Claude Code and all its sub-agents operate entirely inside that clone. Each sub-agent can work on its own branch or worktree within the clone without touching each other or your host at all. When done, you fetch the branches you want.
```bash
Your host (untouched)
       │
       │ read-only mount
       ▼
  Clone-mode sandbox
  ├── private git clone
  │   ├── agent-1 → branch feature/auth
  │   ├── agent-2 → branch feature/profile  
  │   └── agent-3 → branch fix/bug-123
  └── Claude Code orchestrator (agents view)
```
Keep a clone-mode sandbox running across tasks rather than recreating it — the clone and all its branches persist inside the sandbox, so you can keep dispatching new tasks to it and accumulating branches to review.
Since this was specifically a Claude Code feature, it wouldn't apply to a Copilot setup — Copilot CLI doesn't have an equivalent orchestrator/sub-agents view.

# Tearing Down

```bash
# Stop the app stack (inside sandbox or from host)
sbx list
sbx exec copilot-sample-app-spring-ai -- docker compose down

# Destroy the sandbox when done (your code on the host is untouched)
sbx rm copilot-sample-app-spring-ai
```
