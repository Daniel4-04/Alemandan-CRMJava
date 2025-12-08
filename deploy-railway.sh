#!/bin/bash
# ========================================
# Railway Deployment Helper Script
# ========================================
# This script helps initialize and deploy the Alemandan POS Java application to Railway.
# Prerequisites: Railway CLI installed (https://docs.railway.app/develop/cli)
#
# Usage:
#   ./deploy-railway.sh [command]
#
# Commands:
#   init      - Initialize a new Railway project
#   link      - Link to an existing Railway project
#   env       - Set environment variables from .env file
#   deploy    - Deploy the application
#   logs      - View application logs
#   status    - Check deployment status
#   help      - Show this help message

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper functions
info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_railway_cli() {
    if ! command -v railway &> /dev/null; then
        error "Railway CLI is not installed."
        echo "Please install it from: https://docs.railway.app/develop/cli"
        echo ""
        echo "Installation command:"
        echo "  npm i -g @railway/cli"
        echo "  or"
        echo "  brew install railway"
        exit 1
    fi
    info "Railway CLI is installed ✓"
}

init_project() {
    info "Initializing a new Railway project..."
    railway login
    railway init
    info "Project initialized successfully!"
    info "Next steps:"
    echo "  1. Add a MySQL database plugin in Railway dashboard"
    echo "  2. Run: ./deploy-railway.sh env"
    echo "  3. Run: ./deploy-railway.sh deploy"
}

link_project() {
    info "Linking to an existing Railway project..."
    railway login
    railway link
    info "Project linked successfully!"
}

set_env_variables() {
    if [ ! -f ".env" ]; then
        error ".env file not found!"
        warn "Please copy .env.template to .env and fill in your values:"
        echo "  cp .env.template .env"
        echo "  # Edit .env with your actual values"
        exit 1
    fi

    info "Setting environment variables from .env file..."
    
    # Read .env file and set variables (skip comments and empty lines)
    while IFS='=' read -r key value; do
        # Skip comments and empty lines
        if [[ $key =~ ^[[:space:]]*# ]] || [[ -z "$key" ]]; then
            continue
        fi
        
        # Trim whitespace using parameter expansion
        key="${key#"${key%%[![:space:]]*}"}"   # Remove leading whitespace
        key="${key%"${key##*[![:space:]]}"}"   # Remove trailing whitespace
        value="${value#"${value%%[![:space:]]*}"}"  # Remove leading whitespace
        value="${value%"${value##*[![:space:]]}"}"  # Remove trailing whitespace
        
        if [ -n "$key" ] && [ -n "$value" ]; then
            info "Setting $key"
            railway variables --set "$key=$value" 2>/dev/null || warn "Failed to set $key (might already exist or be protected)"
        fi
    done < .env
    
    info "Environment variables set successfully!"
    warn "Remember to verify variables in Railway dashboard"
}

deploy_app() {
    info "Deploying application to Railway..."
    info "This will use the settings from Procfile and Railway configuration"
    railway up
    info "Deployment initiated!"
    info "Check status with: railway logs"
}

show_logs() {
    info "Showing application logs..."
    railway logs
}

show_status() {
    info "Checking deployment status..."
    railway status
}

show_help() {
    cat << EOF
Railway Deployment Helper Script for Alemandan POS

Usage: ./deploy-railway.sh [command]

Commands:
  init      - Initialize a new Railway project (first time setup)
  link      - Link to an existing Railway project
  env       - Set environment variables from .env file
  deploy    - Deploy the application to Railway
  logs      - View application logs
  status    - Check deployment status
  help      - Show this help message

Setup Instructions:
  1. Install Railway CLI: npm i -g @railway/cli
  2. Run: ./deploy-railway.sh init
  3. In Railway dashboard, add MySQL database plugin
  4. Copy .env.template to .env and configure your values
  5. Run: ./deploy-railway.sh env
  6. Run: ./deploy-railway.sh deploy

Common Railway Commands:
  railway login             - Login to Railway
  railway whoami            - Check who is logged in
  railway list              - List all projects
  railway variables         - List environment variables
  railway open              - Open project in browser
  railway logs              - View logs
  railway domain            - Manage domains

For more information, visit: https://docs.railway.app/

EOF
}

# Main script
main() {
    check_railway_cli

    case "${1:-help}" in
        init)
            init_project
            ;;
        link)
            link_project
            ;;
        env)
            set_env_variables
            ;;
        deploy)
            deploy_app
            ;;
        logs)
            show_logs
            ;;
        status)
            show_status
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            error "Unknown command: $1"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
