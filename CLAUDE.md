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
- `./gradlew test` - Run JUnit 5 unit tests
- `./gradlew coverage` - Generate JaCoCo code coverage report for blackjack package
- `./gradlew blackjackCoverage` - Same as above, more explicit
- `./gradlew jacocoTestReport` - Full project coverage report
- `./gradlew runClient` - Launch client for in-game testing  
- `./gradlew gameTestServer` - Run NeoForge game tests

#### Test Coverage
- **Blackjack Package**: 79% instruction coverage, 92% branch coverage
- **BlackjackHandler**: 0% coverage (requires Minecraft server context, but logic tested via HandlerLogicTest)
- **Total Test Count**: 80+ tests covering all game scenarios
- Coverage reports: `build/reports/jacoco/blackjack/index.html`
- Unit tests for Card, Suit, and BlackjackGame classes
- Integration tests for BlackjackHandler game flow
- Handler logic tests for reward calculation, dealer AI, and game transitions
- Parameterized tests for edge cases and comprehensive scenarios

#### Debugging
- Check `runs/client/logs/` for debugging output
- Game state logging available at DEBUG level

### Asset Management
- Textures go in `src/main/resources/assets/casinomod/`
- Language files in `assets/casinomod/lang/en_us.json`
- Block/item models follow standard Minecraft conventions

## Current Development Todos

### P0 Priorities (Critical - Core Blackjack Features)
- [x] Add Double Down functionality to blackjack game ✅
- [x] Display hand values in the blackjack UI ✅
- [x] Show current bet amount in the dealer screen ✅
- [x] Implement action button state management (enable/disable based on game state) ✅
- [ ] Add configurable Soft 17 rule for dealer behavior

### P1 Priorities (High - Enhanced Gameplay)
- [ ] Split Pairs functionality for matching cards
- [ ] Insurance side bet against dealer blackjack
- [ ] Dealer blackjack peek with 10/Ace showing
- [ ] Multi-deck support (2-8 deck shoes)
- [ ] Betting limits enforcement (min/max)

### P2 Priorities (Medium - Polish & Quality of Life)
- [ ] Surrender option for players
- [ ] Win/Loss statistics tracking
- [ ] Game history display (recent hands)
- [ ] Card deal animations
- [ ] Chip system for standardized currency

### P3 Priorities (Low - Advanced Features)
- [ ] Side bets (Perfect Pairs, 21+3)
- [ ] Card counting protection (shuffle timing)
- [ ] Multiple bet amount options
- [ ] Card shuffle animation
- [ ] Enhanced win celebration effects

### P4 Priorities (Nice-to-Have - Extra Polish)
- [ ] Chip stacking sound effects
- [ ] Advanced statistics and analytics
- [ ] Tournament mode
- [ ] Custom deck themes
- [ ] Hot/Cold streak tracking
- Always add tests to things that you add
- When debugging a change make sure to run the client and tell the user what to do in order to test
- When there is a current client running when you are asking to debug you must end the previous client and restart it
- Anytime you make an ingame Minecraft change kill and run the bash.