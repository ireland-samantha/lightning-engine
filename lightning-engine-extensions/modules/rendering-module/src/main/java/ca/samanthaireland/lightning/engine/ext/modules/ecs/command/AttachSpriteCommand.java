package ca.samanthaireland.lightning.engine.ext.modules.ecs.command;

import ca.samanthaireland.lightning.engine.core.command.CommandBuilder;
import ca.samanthaireland.lightning.engine.core.command.EngineCommand;
import ca.samanthaireland.lightning.engine.core.command.PayloadMapper;
import ca.samanthaireland.lightning.engine.core.exception.InvalidParameterException;
import ca.samanthaireland.lightning.engine.ext.modules.domain.service.SpriteService;
import ca.samanthaireland.lightning.engine.ext.modules.ecs.command.dto.AttachSpritePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Command to attach sprite rendering components to an entity.
 *
 * <p>Attaches a sprite with optional parameters for dimensions,
 * rotation, z-index, and visibility.
 */
@Slf4j
public final class AttachSpriteCommand {

    private AttachSpriteCommand() {}

    /**
     * Creates a new attachSprite command.
     *
     * @param spriteService the sprite service for sprite operations
     * @return the configured EngineCommand
     */
    public static EngineCommand create(SpriteService spriteService) {
        return CommandBuilder.newCommand()
                .withName("attachSprite")
                .withSchema(Map.of(
                        "entityId", Long.class,
                        "resourceId", Long.class,
                        "width", Float.class,
                        "height", Float.class,
                        "rotation", Float.class,
                        "zIndex", Float.class,
                        "visible", Float.class
                ))
                .withExecution(payload -> {
                    AttachSpritePayload dto = PayloadMapper.convert(payload, AttachSpritePayload.class);

                    validateRequest(dto);

                    spriteService.attachSprite(
                            dto.entityId(),
                            dto.resourceId(),
                            dto.getWidth(),
                            dto.getHeight(),
                            dto.getRotation(),
                            dto.getZIndex(),
                            dto.isVisible()
                    );
                })
                .build();
    }

    private static void validateRequest(AttachSpritePayload dto) {
        if (!dto.hasEntityId()) {
            throw new InvalidParameterException("attachSprite: missing entityId");
        }

        if (!dto.hasResourceId()) {
            throw new InvalidParameterException("attachSprite: missing resourceId");
        }
    }
}
