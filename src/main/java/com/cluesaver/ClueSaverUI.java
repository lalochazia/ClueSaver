package com.cluesaver;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseManager;
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

	@Inject
	private TierStateSaveManager tierSaveManager;

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

			ClueTier[] tiers = {
				ClueTier.BEGINNER,
				ClueTier.EASY,
				ClueTier.MEDIUM,
				ClueTier.HARD,
				ClueTier.ELITE,
				ClueTier.MASTER
			};

			BufferedImage[] clueScrolls = {
				clueScrollBeginnerImage,
				clueScrollEasyImage,
				clueScrollMediumImage,
				clueScrollHardImage,
				clueScrollEliteImage,
				clueScrollMasterImage
			};

			int currentY = startY;
			int lastIndex = clueScrolls.length - 1;
			while (lastIndex >= 0 && clueScrolls[lastIndex] == null) {
				lastIndex--;
			}

			if (clueScrolls[0] != null) {
				graphics.drawImage(clueScrolls[0], startX, currentY, null);
				currentY += clueScrolls[0].getHeight() + padding;
			}

			for (int i = 1; i <= lastIndex; i++) {
				if (clueScrolls[i] != null) {
					int maxClueCount = clueSaverUtils.getMaxClueCount(tiers[i-1], client);
					if (pipImage != null && maxClueCount > 0) {
						int pipX = startX - 5;
						int pipStartY = currentY;

						String savingCause = clueSaverPlugin.getTierSavingCause(tiers[i-1]);
						int heldClueCount = 0;

						if (savingCause != null) {
							ScrollBoxState boxState = clueStates.getBoxStateFromTier(tiers[i-1]);
							ClueScrollState clueState = clueStates.getClueStateFromTier(tiers[i-1]);
							heldClueCount = boxState.getTotalCount();
							if (clueState.getLocation() == ClueLocation.INVENTORY) {
								heldClueCount += 1;
							}
						}

						for (int pip = maxClueCount - 1; pip >= 0; pip--) {
							int pipY = pipStartY - ((pip + 1) * (pipImage.getHeight() - 1)) - 8;

							if (pip < heldClueCount) {
								graphics.drawImage(pipGreenImage, pipX, pipY, null);
							} else {
								graphics.drawImage(pipImage, pipX, pipY, null);
							}
						}
					}

					graphics.drawImage(clueScrolls[i], startX, currentY, null);
					currentY += clueScrolls[i].getHeight() + padding;
				}
			}

			if (lastIndex >= 0) {
				int maxClueCount = clueSaverUtils.getMaxClueCount(tiers[lastIndex], client);
				if (pipImage != null && maxClueCount > 0) {
					int pipX = startX - 5;
					int pipStartY = currentY;

					String savingCause = clueSaverPlugin.getTierSavingCause(tiers[lastIndex]);

					int heldClueCount = 0;

					if (savingCause != null) {
						ScrollBoxState boxState = clueStates.getBoxStateFromTier(tiers[lastIndex]);
						ClueScrollState clueState = clueStates.getClueStateFromTier(tiers[lastIndex]);
						heldClueCount = boxState.getTotalCount();
						if (clueState.getLocation() == ClueLocation.INVENTORY) {
							heldClueCount += 1;
						}
					}


					for (int pip = maxClueCount - 1; pip >= 0; pip--) {
						int pipY = pipStartY - ((pip + 1) * (pipImage.getHeight() - 1)) - 8;

						if (pip < heldClueCount) {
							graphics.drawImage(pipGreenImage, pipX, pipY, null);
						} else {
							graphics.drawImage(pipImage, pipX, pipY, null);
						}
					}
				}
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
}