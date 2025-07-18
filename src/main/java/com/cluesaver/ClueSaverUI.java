package com.cluesaver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

@Slf4j
public class ClueSaverUI extends Overlay implements MouseListener {
	private final Client client;
	private final ClientThread clientThread;
	private final ClueSaverConfig config;
	private final ClueSaverUtils clueSaverUtils;
	private final ClueSaverPlugin clueSaverPlugin;
	private final ClueStates clueStates;
	private boolean shouldDraw = false;
	private boolean isButtonHovered = false;
	private boolean isExpanded = false;
	private Rectangle buttonBounds;
	private Rectangle beginnerIconBounds;
	private Rectangle easyIconBounds;
	private Rectangle mediumIconBounds;
	private Rectangle hardIconBounds;
	private Rectangle eliteIconBounds;
	private Rectangle masterIconBounds;
	private final BufferedImage closedUIImage;
	private final BufferedImage expandedUIImage;
	private final BufferedImage buttonUIImage;
	private final BufferedImage buttonUIHoveredImage;
	private final BufferedImage clueScrollBeginnerImage;
	private final BufferedImage clueScrollEasyImage;
	private final BufferedImage clueScrollMediumImage;
	private final BufferedImage clueScrollHardImage;
	private final BufferedImage clueScrollEliteImage;
	private final BufferedImage clueScrollMasterImage;
	private final BufferedImage pipImage;
	private final BufferedImage pipGreenImage;
	private final BufferedImage pipOrangeImage;
	private final BufferedImage pipRedImage;
	private BufferedImage activeClueSaver;
	private BufferedImage invIcon;
	private BufferedImage bankIcon;

	@Inject
	public ClueSaverUI(Client client, ClientThread clientThread,
					   ClueSaverUtils clueSaverUtils, ClueSaverPlugin clueSaverPlugin,
					   ClueStates clueStates, ClueSaverConfig config) {
		this.config = config;
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
		pipOrangeImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/pipOrange.png");
		pipRedImage = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/pipRed.png");
		invIcon = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/invIcon.png");
		bankIcon = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/bankIcon.png");
		activeClueSaver = ImageUtil.loadImageResource(getClass(), "/com/cluesaver/activeClueSaver.png");
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

		int visibleTierCount = 0;
		for (ClueTier tier : ClueTier.values()) {
			if (shouldShowTier(tier)) {
				visibleTierCount++;
			}
		}

		BufferedImage firstClueImage = getClueImage(ClueTier.values()[0]);
		int clueImageHeight = firstClueImage != null ? firstClueImage.getHeight() : 0;
		int padding = 2;
		int totalHeight = (clueImageHeight + padding) * visibleTierCount;

		final int closedUIX = 0;
		final int closedUIY = (client.getCanvasHeight() - totalHeight) / 3;

		graphics.drawImage(closedUIImage,
			closedUIX, closedUIY,
			closedUIX + closedUIImage.getWidth(), closedUIY + totalHeight,
			0, 0,
			closedUIImage.getWidth(), closedUIImage.getHeight(),
			null);

		if (isExpanded) {
			final int expandedUIX = closedUIX + closedUIImage.getWidth();
			final int expandedUIY = closedUIY;
			final int startX = expandedUIX + 4;
			final int startY = expandedUIY + 3;

			int currentY = startY;

			for (ClueTier tier : ClueTier.values()) {
				if (!shouldShowTier(tier)) {
					continue;
				}
				BufferedImage clueImage = getClueImage(tier);
				if (clueImage == null) continue;
				int nextY = currentY + clueImage.getHeight() + padding;
				graphics.drawImage(clueImage, startX, currentY, null);
				int totalBoxes = 0;
				boolean hasClueInInventory = false;
				boolean hasClueInBank = false;
				// This needs to be fixed so badly
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
							} catch (Exception e) {
							}
						}
						if (part.contains("Clue in inventory")) {
							hasClueInInventory = true;
							totalBoxes++;
						} else if (part.contains("Clue in bank")) {
							hasClueInBank = true;
							totalBoxes++;
						}
					}

					if (hasClueInInventory && invIcon != null) {
						int invIconX = startX + clueImage.getWidth() - invIcon.getWidth();
						int invIconY = currentY + clueImage.getHeight() - invIcon.getHeight();
						graphics.drawImage(invIcon, invIconX, invIconY, null);
					}

					if (hasClueInBank && bankIcon != null) {
						int bankIconX = startX + clueImage.getWidth() - bankIcon.getWidth();
						int bankIconY = currentY + clueImage.getHeight() - bankIcon.getHeight();
						graphics.drawImage(bankIcon, bankIconX, bankIconY, null);
					}
				}

				int pipX = startX - 5;
				int pipStartY = currentY + clueImage.getHeight();
				int maxClueCount = clueSaverUtils.getMaxClueCount(tier, client);

				if (totalBoxes >= maxClueCount && activeClueSaver != null) {
					graphics.drawImage(activeClueSaver, startX, currentY, null);
				}

				for (int pip = maxClueCount - 1; pip >= 0; pip--) {
					int pipY = pipStartY - ((pip + 1) * (pipImage.getHeight() - 1)) - 6;
					if (totalBoxes >= maxClueCount) {
						graphics.drawImage(pipRedImage, pipX, pipY, null);
					} else if (maxClueCount - totalBoxes == 1 && pip < totalBoxes) {
						graphics.drawImage(pipOrangeImage, pipX, pipY, null);
					} else if (pip < totalBoxes) {
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
			(isExpanded ? 45 : 0);
		final int buttonUIY = closedUIY + 8;
		buttonBounds = new Rectangle(buttonUIX, buttonUIY,
			buttonUIImage.getWidth(), buttonUIImage.getHeight());
		BufferedImage buttonToDraw = isButtonHovered ? buttonUIHoveredImage : buttonUIImage;
		graphics.drawImage(buttonToDraw, buttonUIX, buttonUIY, null);

		return null;
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

	private boolean shouldShowTier(ClueTier tier) {
		switch (tier) {
			case BEGINNER:
				return config.showBeginnerInfo();
			case EASY:
				return config.showEasyInfo();
			case MEDIUM:
				return config.showMediumInfo();
			case HARD:
				return config.showHardInfo();
			case ELITE:
				return config.showEliteInfo();
			case MASTER:
				return config.showMasterInfo();
			default:
				return false;
		}
	}

	public void setVisible(boolean visible) {
		this.shouldDraw = visible;
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