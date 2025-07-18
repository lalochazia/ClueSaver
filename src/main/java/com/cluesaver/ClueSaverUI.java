package com.cluesaver;

import java.time.temporal.ChronoUnit;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseManager;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Point;
import java.awt.event.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.input.MouseListener;

@Slf4j
public class ClueSaverUI extends Overlay implements MouseListener {
	private final BufferedImage closedUIImage;
	private final BufferedImage buttonUIImage;
	private final BufferedImage buttonUIHoveredImage;
	private final BufferedImage expandedUIImage;
	private final BufferedImage clueScrollBeginnerImage;
	private final BufferedImage clueScrollEasyImage;
	private final BufferedImage clueScrollMediumImage;
	private final BufferedImage clueScrollHardImage;
	private final BufferedImage clueScrollEliteImage;
	private final BufferedImage clueScrollMasterImage;
	private final ClueSaverUtils clueSaverUtils;
	private final ClueSaverPlugin clueSaverPlugin;
	private final ClueStates clueStates;
	private final BufferedImage pipImage;
	private final BufferedImage pipGreenImage;
	private boolean shouldDraw = false;
	private boolean isButtonHovered = false;
	private boolean isExpanded = false;
	private final Client client;
	private final ClientThread clientThread;
	private Rectangle buttonBounds;
	private Rectangle beginnerIconBounds;
	private Rectangle easyIconBounds;
	private Rectangle mediumIconBounds;
	private Rectangle hardIconBounds;
	private Rectangle eliteIconBounds;
	private Rectangle masterIconBounds;

	@Inject
	public ClueSaverUI(Client client, ClientThread clientThread,
					   ClueSaverUtils clueSaverUtils, ClueSaverPlugin clueSaverPlugin,
					   ClueStates clueStates) {
		this.client = client;
		this.clientThread = clientThread;
		this.clueSaverUtils = clueSaverUtils;
		this.clueSaverPlugin = clueSaverPlugin;
		this.clueStates = clueStates;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(100);

		closedUIImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/ClosedUI.png");
		buttonUIImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/buttonUI.png");
		buttonUIHoveredImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/buttonUIhovered.png");
		expandedUIImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/UIexpanded.png");
		clueScrollBeginnerImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollBeginner.png");
		clueScrollEasyImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollEasy.png");
		clueScrollMediumImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollMedium.png");
		clueScrollHardImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollHard.png");
		clueScrollEliteImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollElite.png");
		clueScrollMasterImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/cluescrollMaster.png");
		pipImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/pip.png");
		pipGreenImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/pipGreen.png");
		if (closedUIImage == null || buttonUIImage == null ||
			buttonUIHoveredImage == null || expandedUIImage == null) {
			log.debug("Failed to load one or more UI images");
		} else {
			log.debug("Successfully loaded all UI images");
		}
		if (pipImage == null || pipGreenImage == null) {
			log.debug("Failed to load pip images - pip: {}, pipGreen: {}",
				pipImage != null, pipGreenImage != null);
		} else {
			log.debug("Successfully loaded pip images");
		}
		log.info("ClueSaverUI initialized with:");
		log.info("ClueSaverUtils: {}", clueSaverUtils != null ? "present" : "null");
		log.info("ClueSaverPlugin: {}", clueSaverPlugin != null ? "present" : "null");
		log.info("ClueStates: {}", clueStates != null ? "present" : "null");
	}


	public void setVisible(boolean visible) {
		this.shouldDraw = visible;
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (!shouldDraw || closedUIImage == null || buttonUIImage == null ||
			buttonUIHoveredImage == null || expandedUIImage == null) {
			return null;
		}

		if (clueStates == null) {
			log.info("ClueStates is null in render method");
			return null;
		}

		final int closedUIX = 0;
		final int closedUIY = (client.getCanvasHeight() - closedUIImage.getHeight()) / 2;
		graphics.drawImage(closedUIImage, closedUIX, closedUIY, null);

		if (isExpanded) {
			final int expandedUIX = closedUIX + closedUIImage.getWidth();
			final int expandedUIY = closedUIY;
			graphics.drawImage(expandedUIImage, expandedUIX, expandedUIY, null);
			final int startX = expandedUIX + 8;
			final int startY = expandedUIY + 3;
			final int padding = 2;

			int currentY = startY;

			graphics.setColor(Color.WHITE);
			graphics.setFont(new Font("Arial", Font.BOLD, 12));

			for (ClueTier tier : ClueTier.values()) {
				BufferedImage clueImage = getClueImage(tier);
				if (clueImage == null) continue;
				int nextY = currentY + clueImage.getHeight() + padding;
				graphics.drawImage(clueImage, startX, currentY, null);
				int totalBoxes = 0;
				String savingCause = clueSaverPlugin.getTierSavingCause(tier, true);
				if (savingCause != null) {
					String cleanedCause = savingCause
						.replaceAll("<col=[^>]+>", "")
						.replaceAll("</col>", "");

					String[] parts = cleanedCause.split(" \\| ");
					for (String part : parts) {
						if (part.contains("Scroll Boxes:")) {
							try {
								String countStr = part.substring(part.indexOf("Scroll Boxes:") + "Scroll Boxes:".length()).trim();
								totalBoxes = Integer.parseInt(countStr);
								break;
							} catch (Exception e) {
							}
						}
					}
				}

				int pipX = startX - 5;
				int pipStartY = currentY + clueImage.getHeight();
				int maxClueCount = clueSaverUtils.getMaxClueCount(tier, client);

				for (int pip = maxClueCount - 1; pip >= 0; pip--) {
					int pipY = pipStartY - ((pip + 1) * (pipImage.getHeight() - 1)) - 6;
					if (pip < totalBoxes) {
						graphics.drawImage(pipGreenImage, pipX, pipY, null);
					} else {
						graphics.drawImage(pipImage, pipX, pipY, null);
					}
				}
				updateIconBounds(tier, startX, currentY, clueImage);
				currentY = nextY;
			}
		}

		final int buttonUIX = closedUIX + closedUIImage.getWidth() +
			(isExpanded ? expandedUIImage.getWidth() : 0);
		final int buttonUIY = closedUIY + 10;
		buttonBounds = new Rectangle(buttonUIX, buttonUIY,
			buttonUIImage.getWidth(), buttonUIImage.getHeight());
		BufferedImage buttonToDraw = isButtonHovered ? buttonUIHoveredImage : buttonUIImage;
		graphics.drawImage(buttonToDraw, buttonUIX, buttonUIY, null);

		return null;
	}

	private Rectangle getIconBounds(ClueTier tier) {
		switch (tier) {
			case BEGINNER: return beginnerIconBounds;
			case EASY: return easyIconBounds;
			case MEDIUM: return mediumIconBounds;
			case HARD: return hardIconBounds;
			case ELITE: return eliteIconBounds;
			case MASTER: return masterIconBounds;
			default: return new Rectangle();
		}
	}

	private void updateIconBounds(ClueTier tier, int x, int y, BufferedImage image) {
		Rectangle bounds = new Rectangle(x, y, image.getWidth(), image.getHeight());
		switch (tier) {
			case BEGINNER:
				beginnerIconBounds = bounds;
				break;
			case EASY:
				easyIconBounds = bounds;
				break;
			case MEDIUM:
				mediumIconBounds = bounds;
				break;
			case HARD:
				hardIconBounds = bounds;
				break;
			case ELITE:
				eliteIconBounds = bounds;
				break;
			case MASTER:
				masterIconBounds = bounds;
				break;
		}
	}

	private BufferedImage getClueImage(ClueTier tier) {
		switch (tier) {
			case BEGINNER: return clueScrollBeginnerImage;
			case EASY: return clueScrollEasyImage;
			case MEDIUM: return clueScrollMediumImage;
			case HARD: return clueScrollHardImage;
			case ELITE: return clueScrollEliteImage;
			case MASTER: return clueScrollMasterImage;
			default: return null;
		}
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent e) {
		if (buttonBounds != null && buttonBounds.contains(e.getPoint())) {
			isExpanded = !isExpanded;
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent e) {
		if (buttonBounds != null && buttonBounds.contains(e.getPoint())) {
			e.consume();
		}
		return e;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent e) {
		if (buttonBounds != null) {
			boolean wasHovered = isButtonHovered;
			isButtonHovered = buttonBounds.contains(e.getPoint());
			if (wasHovered != isButtonHovered) {
				e.consume();
			}
		}
		return e;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent e) {
		return e;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent e) {
		isButtonHovered = false;
		return e;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent e) {
		return e;
	}

	private void logDebugInfo() {
		if (clueStates == null) {
			log.info("ClueStates is null");
			return;
		}

		log.info("=== ClueStates Status Update ===");
		for (ClueTier tier : ClueTier.values()) {
			ClueScrollState clueState = clueStates.getClueStateFromTier(tier);
			ScrollBoxState boxState = clueStates.getBoxStateFromTier(tier);
			String savingCause = clueSaverPlugin.getTierSavingCause(tier, true);
			int maxClueCount = clueSaverUtils.getMaxClueCount(tier, client);

			StringBuilder status = new StringBuilder();
			status.append(String.format("[%s] ", tier));

			status.append(String.format("Max: %d", maxClueCount));

			if (clueState != null) {
				status.append(String.format(", Clue: %s",
					clueState.getLocation() != ClueLocation.UNKNOWN ? clueState.getLocation() : "none"));
			}

			if (boxState != null) {
				int totalBoxes = boxState.getTotalCount();
				status.append(String.format(", Boxes: %d", totalBoxes));
			}

			if (savingCause != null && !savingCause.isEmpty()) {
				String cleanCause = savingCause
					.replaceAll("<col=[^>]+>", "")
					.replaceAll("</col>", "")
					.replaceAll("<br>", " | ")
					.trim();
				status.append(String.format(" | %s", cleanCause));
			}

			log.info(status.toString());
		}
		log.info("===========================");
	}
}