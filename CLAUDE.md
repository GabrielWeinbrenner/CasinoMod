# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CasinoMod is a Minecraft mod built with NeoForge that adds casino gaming functionality to Minecraft. The mod currently implements a blackjack game through a dealer block interface with a custom GUI.

## Development Commands

### Build and Run
- `./gradlew runClient` - Launch Minecraft client with the mod for testing
- `./gradlew build` - Build the mod jar file
- `./gradlew clean` - Clean build artifacts

### Code Quality
- `./gradlew spotlessApply` - Format Java code using Google Java Format
- `./gradlew googleJavaFormat` - Alternative formatting command
- `./gradlew fc` - Custom task that formats code and launches client

### Development Setup
- `./gradlew --refresh-dependencies` - Refresh local dependency cache
- IDE: IntelliJ IDEA or Eclipse recommended

## Architecture Overview

### Core Mod Structure
- **Main Mod Class**: `CasinoMod.java` - Entry point with NeoForge event handling
- **Client Setup**: `CasinoModClient.java` - Client-side initialization
- **Configuration**: `Config.java` - Mod configuration using NeoForge's config system

### Game Implementation
- **Blackjack Game Logic**: `blackjack/BlackjackGame.java` - Serializable game state with phases (WAITING, PLAYER_TURN, DEALER_TURN, FINISHED)
- **Card System**: `blackjack/Card.java` and `blackjack/Suit.java` - Playing card representation
- **Game Handler**: `blackjack/handler/BlackjackHandler.java` - Server-side game logic coordination

### Block System
- **Dealer Block**: `block/custom/DealerBlock.java` - Interactive block for casino games
- **Block Entity**: `block/custom/DealerBlockEntity.java` - Stores game state and handles networking
- **Block Registration**: `block/ModBlocks.java` and `block/entity/ModBlockEntities.java`

### User Interface
- **Dealer Screen**: `screen/custom/DealerScreen.java` - Custom GUI with card rendering and game controls
- **Menu System**: `screen/custom/DealerMenu.java` and `screen/ModMenuTypes.java` - Container-based UI handling

### Networking
- **Packet System**: `network/DealerButtonPacket.java` - Client-to-server communication for game actions
- **Message Registration**: `network/ModMessages.java` - NeoForge packet registration

### Utilities
- **Task Scheduling**: `util/ServerTaskScheduler.java` - Server tick-based task execution

## Key Technical Details

### NeoForge Version
- Minecraft: 1.21.8
- NeoForge: 21.8.13
- Java: 21 (required)

### Game State Management
- Games are serialized using NeoForge's `ValueIOSerializable` system
- State persists with block entities using data attachments
- Game phases control UI and player actions

### Rendering System
- Custom card textures in `assets/casinomod/textures/gui/dealer_block/cards/`
- Scalable GUI rendering with dynamic sizing
- Card back/front logic for dealer's hidden card

### Code Style
- Google Java Format enforced via Spotless
- Import ordering: java, javax, com, org, others
- No unused imports policy

## Development Guidelines

### Adding New Games
1. Create game logic class implementing `ValueIOSerializable`
2. Add packet types for player actions in `network/`
3. Extend or create new screen/menu classes for UI
4. Register new components in respective `Mod*.java` classes

### Testing
- Use `./gradlew runClient` for in-game testing
- Check `runs/client/logs/` for debugging output
- Game state logging available at DEBUG level

### Asset Management
- Textures go in `src/main/resources/assets/casinomod/`
- Language files in `assets/casinomod/lang/en_us.json`
- Block/item models follow standard Minecraft conventions