#!/bin/bash

# Build and install script for UltimateInventory Bukkit Plugin
# Usage: ./build-and-install.sh [server-plugins-path]
# If no path is provided, defaults to $HOME/Minecraft/Servers/Paper/plugins
# You can override by providing a path as an argument or setting SERVER_PLUGINS_PATH env var

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Exit on error for build, but not for server detection
set -e

# Default server plugins path (uses current user's home directory)
DEFAULT_SERVER_PLUGINS_PATH="$HOME/Minecraft/Servers/Paper/plugins"
DEFAULT_SERVER_DIR="$HOME/Minecraft/Servers/Paper"

# Get server plugins path from argument, environment variable, or use default
SERVER_PLUGINS_PATH="${1:-${SERVER_PLUGINS_PATH:-$DEFAULT_SERVER_PLUGINS_PATH}}"
SERVER_DIR="${SERVER_DIR:-$DEFAULT_SERVER_DIR}"

# Function to find the server process
find_server_process() {
    # Look for java processes that might be the server
    # First, try to find processes running from the server directory
    for pid in $(ps aux | grep "[j]ava" | grep -E "(paper|spigot|server\.jar)" | awk '{print $2}'); do
        if [ -n "$pid" ]; then
            # Check the working directory of the process
            cwd=$(lsof -p "$pid" 2>/dev/null | grep cwd | awk '{print $9}' | head -1)
            if [ -n "$cwd" ] && [[ "$cwd" == "$SERVER_DIR"* ]]; then
                ps aux | grep "^[^ ]* *$pid " | grep -v grep
                return 0
            fi
        fi
    done
    
    # Fallback: just look for any java process with paper/spigot in the command
    ps aux | grep -i "[j]ava.*paper\|[j]ava.*spigot" | grep -v grep | head -1
}

# Function to get the terminal window for a process
get_terminal_for_process() {
    local pid=$1
    if [ -z "$pid" ]; then
        return
    fi
    
    # On macOS, try to find the terminal using osascript
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # Get the parent process (might be the terminal)
        local ppid=$(ps -o ppid= -p "$pid" 2>/dev/null | tr -d ' ')
        if [ -n "$ppid" ]; then
            local pname=$(ps -o comm= -p "$ppid" 2>/dev/null)
            if [[ "$pname" == *"Terminal"* ]] || [[ "$pname" == *"iTerm"* ]]; then
                echo "$ppid"
            fi
        fi
    fi
}

# Function to gracefully stop the server
stop_server() {
    local server_pid=$1
    if [ -z "$server_pid" ]; then
        return 0
    fi
    
    echo "Stopping server (PID: $server_pid)..."
    
    # Temporarily disable exit on error for kill commands
    set +e
    
    # Try graceful shutdown first (SIGTERM)
    kill -TERM "$server_pid" 2>/dev/null || return 0
    
    # Wait up to 30 seconds for graceful shutdown
    local count=0
    while kill -0 "$server_pid" 2>/dev/null && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
    done
    
    # If still running, force kill
    if kill -0 "$server_pid" 2>/dev/null; then
        echo "Server didn't stop gracefully, forcing shutdown..."
        kill -KILL "$server_pid" 2>/dev/null || true
        sleep 1
    fi
    
    # Re-enable exit on error
    set -e
    
    echo "Server stopped."
}

# Function to restart the server
restart_server() {
    # Check if paper.jar exists
    local paper_jar="$SERVER_DIR/paper.jar"
    
    if [ ! -f "$paper_jar" ]; then
        echo "Warning: Could not find paper.jar in $SERVER_DIR"
        echo "Please start the server manually."
        return 1
    fi
    
    echo "Starting server in new terminal..."
    
    # Build the exact command from the old script
    local start_cmd="cd '$SERVER_DIR' && echo 'Starting Minecraft Server...' && java -Xmx2G -Xms2G -jar '$paper_jar' nogui"
    
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS: Open new Terminal window with the command
        osascript -e "tell application \"Terminal\" to do script \"$start_cmd\""
    else
        # Linux: Try to use xterm or gnome-terminal
        if command -v gnome-terminal &> /dev/null; then
            gnome-terminal -- bash -c "$start_cmd; exec bash"
        elif command -v xterm &> /dev/null; then
            xterm -e "bash -c \"$start_cmd\"" &
        else
            echo "Warning: Could not find a terminal emulator. Please start the server manually."
            echo "  $start_cmd"
        fi
    fi
}

echo "Building UltimateInventory plugin..."
echo ""

# Clean and build with Maven
mvn clean package

# Find the built JAR (should be in target/)
BUILT_JAR=$(find target -name "UltimateInventory-*.jar" -type f ! -name "*-sources.jar" ! -name "original-*.jar" | head -1)

if [ -z "$BUILT_JAR" ]; then
    echo "Error: Could not find built JAR file!"
    echo "Expected pattern: UltimateInventory-*.jar"
    echo "Found in target/:"
    ls -la target/*.jar 2>/dev/null || echo "  (no JARs found)"
    exit 1
fi

JAR_NAME=$(basename "$BUILT_JAR")
echo ""
echo "✓ Build successful!"
echo "  JAR: $BUILT_JAR"

# Copy to server plugins folder
if [ -n "$SERVER_PLUGINS_PATH" ]; then
    # Expand ~ to home directory
    SERVER_PLUGINS_PATH="${SERVER_PLUGINS_PATH/#\~/$HOME}"
    
    # Check if path exists
    if [ ! -d "$SERVER_PLUGINS_PATH" ]; then
        echo ""
        echo "Warning: Server plugins directory does not exist: $SERVER_PLUGINS_PATH"
        echo "Creating directory..."
        mkdir -p "$SERVER_PLUGINS_PATH"
    fi
    
    # Remove old version if it exists
    if [ -f "$SERVER_PLUGINS_PATH/$JAR_NAME" ]; then
        echo "Removing old version from server..."
        rm -f "$SERVER_PLUGINS_PATH/$JAR_NAME"
    fi
    
    # Copy new JAR
    echo "Copying $JAR_NAME to $SERVER_PLUGINS_PATH/"
    cp "$BUILT_JAR" "$SERVER_PLUGINS_PATH/"
    
    echo ""
    echo "✓ Plugin installed to server!"
    echo "  Location: $SERVER_PLUGINS_PATH/$JAR_NAME"
    
    # Check if server is running and restart it
    # Temporarily disable exit on error for server detection
    set +e
    echo ""
    SERVER_PROCESS=$(find_server_process)
    set -e
    
    if [ -n "$SERVER_PROCESS" ]; then
        SERVER_PID=$(echo "$SERVER_PROCESS" | awk '{print $2}')
        echo "Found running server (PID: $SERVER_PID)"
        
        # Stop the server gracefully
        stop_server "$SERVER_PID"
        
        # Wait a moment for cleanup
        sleep 2
        
        # Restart the server
        restart_server
    else
        echo "Server is not currently running."
        read -p "Would you like to start it? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            restart_server
        fi
    fi
fi

echo ""
echo "Build complete!"

