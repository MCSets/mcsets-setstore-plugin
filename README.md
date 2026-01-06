# MCSets SetStore Plugin

Official PaperMC plugin for [MCSets SetStore](https://mcsets.com) - automatic delivery of store purchases to your Minecraft server.

## Features

- **Automatic Delivery**: Automatically executes commands when purchases are made
- **Real-time Notifications**: WebSocket support for instant delivery notifications
- **Fallback Polling**: Automatic fallback to polling if WebSocket is unavailable
- **Player Verification**: `/verify` command for players to link their Minecraft accounts
- **Configurable Logging**: Toggle logging for debugging and monitoring
- **Offline Queueing**: Queue deliveries for offline players

## Requirements

- **Minecraft**: 1.20.4+
- **Server Software**: Paper, Purpur, or compatible forks
- **Java**: 17 or higher

## Installation

1. Download the latest release from [releases](https://github.com/mcsets/mcsets-setstore-plugin/releases)
2. Place the JAR file in your server's `plugins` folder
3. Start/restart your server
4. Configure your API key in `plugins/SetStore/config.yml`

## Configuration

After first run, edit `plugins/SetStore/config.yml`:

```yaml
# Your SetStore API key (get from https://mcsets.com/dashboard/servers)
api-key: "your-api-key-here"

# API settings
api:
  base-url: "https://mcsets.com/api/v1/setstore"
  timeout: 30

# Server identification (auto-detected if empty)
server:
  ip: ""
  port: 0

# WebSocket for real-time notifications
websocket:
  enabled: true
  reconnect-delay: 5
  max-reconnect-attempts: 5

# Fallback polling
polling:
  enabled: true
  interval: 60

# Heartbeat to keep server online
heartbeat:
  interval: 300

# Delivery settings
delivery:
  command-delay: 5
  require-online: false
  queue-offline: true

# Logging
logging:
  level: "INFO"
  log-requests: false
  log-deliveries: true
  log-commands: true

# Debug mode
debug: false
```

## Commands

### Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/verify` | `mcsets.verify` | Generate a verification code to link your Minecraft account |

### Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/setstore status` | `mcsets.admin` | Show connection status |
| `/setstore queue` | `mcsets.admin` | Process pending deliveries |
| `/setstore reconnect` | `mcsets.admin` | Reconnect to SetStore |
| `/setstore debug` | `mcsets.admin` | Toggle debug mode |
| `/setstore logging [option]` | `mcsets.admin` | Configure logging (requests/deliveries/commands/all/none) |
| `/setstore reload` | `mcsets.admin` | Reload configuration |
| `/setstore help` | `mcsets.admin` | Show help |

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `mcsets.verify` | true | Allows using the /verify command |
| `mcsets.admin` | op | Access to admin commands |
| `mcsets.notify` | op | Receive delivery notifications |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/mcsets/mcsets-setstore-plugin.git
cd mcsets-setstore-plugin

# Build with Maven
mvn clean package

# The JAR will be in target/mcsets-setstore-plugin-1.0.0.jar
```

## API Endpoints Used

The plugin communicates with the following SetStore API endpoints:

- `POST /connect` - Register server on startup
- `GET /queue` - Fetch pending deliveries
- `POST /deliver` - Report delivery results
- `POST /online` - Report online players
- `POST /heartbeat` - Keep server marked as online
- `POST /verify` - Generate player verification codes

## Troubleshooting

### WebSocket Connection Failed

If WebSocket fails to connect, the plugin will automatically fall back to polling. This is normal if your hosting doesn't support WebSocket connections. Check the console for:

```
[SetStore] WebSocket server not available. Disabling WebSocket and using polling instead.
```

### API Connection Failed

1. Verify your API key is correct in `config.yml`
2. Ensure your server can reach `mcsets.com` on port 443
3. Check firewall settings

### Deliveries Not Executing

1. Run `/setstore status` to check connection
2. Run `/setstore queue` to manually process pending deliveries
3. Enable debug mode with `/setstore debug` for detailed logs

## Support

- **Documentation**: [https://mcsets.com/docs](https://mcsets.com/docs)
- **Issues**: [GitHub Issues](https://github.com/mcsets/mcsets-setstore-plugin/issues)
- **Discord**: [MCSets Discord](https://discord.gg/mcsets)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Credits

Developed by [MCSets](https://mcsets.com)
