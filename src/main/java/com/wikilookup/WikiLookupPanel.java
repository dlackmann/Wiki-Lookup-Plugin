package com.wikilookup;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import net.runelite.client.util.LinkBrowser;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

public class WikiLookupPanel extends PluginPanel
{
	private static final int MAX_EXTRACT = 900;

	private static final Color GOLD           = new Color(255, 200, 60);
	private static final Color ALCH_ORANGE   = new Color(230, 120, 40);
	private static final Color MEMBERS_GREEN = new Color(80, 200, 100);
	private static final Color FTP_YELLOW    = new Color(255, 220, 60);
	private static final Color QUEST_AMBER   = new Color(255, 165, 0);
	private static final Color STAT_GRAY     = new Color(160, 160, 160);
	private static final Color POSITIVE      = new Color(0, 200, 83);
	private static final Color NEGATIVE      = new Color(210, 60, 60);
	private static final Color VS_BLUE       = new Color(160, 210, 255);

	private static final Font INFO_FONT  = FontManager.getRunescapeFont();
	private static final Font STAT_FONT  = FontManager.getRunescapeSmallFont();
	private static final Font TITLE_FONT = FontManager.getRunescapeBoldFont().deriveFont(16f);

	private final WikiLookupConfig config;

	// Header
	private final JLabel iconLabel;
	private final JLabel titleLabel;

	// Info strip — row 1: members status
	private final JLabel membersLabel;

	// Info strip — row 2: Grand Exchange price (two labels for two colors)
	private final JLabel geKeyLabel;    // "Grand Exchange:"
	private final JLabel gePriceLabel;  // the amount

	// Info strip — row 3: High Alch price
	private final JPanel alchRow;
	private final JLabel alchKeyLabel;  // "High Alch:"
	private final JLabel alchLabel;     // the amount

	// Quest warning
	private final JPanel questPanel;

	// Equipment stats — outer wrapper that can be hidden
	private final JPanel statsWrapper;

	// Extract
	private final JTextArea extractArea;

	// Footer
	private final JButton openWikiButton;
	private final JLabel statusLabel;

	private String currentWikiUrl;

	public WikiLookupPanel(WikiLookupConfig config)
	{
		super(false);
		this.config = config;

		setLayout(new BorderLayout(0, 0));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));

		// ── TOP ───────────────────────────────────────────────────────────
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Header: icon + title
		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);
		header.setBorder(new EmptyBorder(0, 0, 6, 0));
		header.setAlignmentX(Component.LEFT_ALIGNMENT);

		iconLabel = new JLabel();
		iconLabel.setPreferredSize(new Dimension(36, 36));
		iconLabel.setMinimumSize(new Dimension(36, 36));
		iconLabel.setHorizontalAlignment(SwingConstants.CENTER);

		titleLabel = new JLabel("Right-click an item to look it up");
		titleLabel.setForeground(Color.WHITE);
		titleLabel.setFont(TITLE_FONT);
		header.add(iconLabel, BorderLayout.WEST);
		header.add(titleLabel, BorderLayout.CENTER);
		top.add(header);

		// Info row 1: Members / Free to Play
		JPanel infoRow1 = new JPanel();
		infoRow1.setLayout(new BoxLayout(infoRow1, BoxLayout.X_AXIS));
		infoRow1.setBackground(ColorScheme.DARK_GRAY_COLOR);
		infoRow1.setBorder(new EmptyBorder(0, 0, 2, 0));
		infoRow1.setAlignmentX(Component.LEFT_ALIGNMENT);

		membersLabel = new JLabel("—");
		membersLabel.setForeground(STAT_GRAY);
		membersLabel.setFont(INFO_FONT);

		infoRow1.add(membersLabel);
		infoRow1.add(Box.createHorizontalGlue());
		top.add(infoRow1);

		// Info row 2: Grand Exchange price
		JPanel infoRow2 = new JPanel();
		infoRow2.setLayout(new BoxLayout(infoRow2, BoxLayout.X_AXIS));
		infoRow2.setBackground(ColorScheme.DARK_GRAY_COLOR);
		infoRow2.setBorder(new EmptyBorder(0, 0, 2, 0));
		infoRow2.setAlignmentX(Component.LEFT_ALIGNMENT);

		geKeyLabel = new JLabel("Grand Exchange: ");
		geKeyLabel.setForeground(STAT_GRAY);
		geKeyLabel.setFont(INFO_FONT);

		gePriceLabel = new JLabel("—");
		gePriceLabel.setForeground(GOLD);
		gePriceLabel.setFont(INFO_FONT);

		infoRow2.add(geKeyLabel);
		infoRow2.add(gePriceLabel);
		infoRow2.add(Box.createHorizontalGlue());
		top.add(infoRow2);

		// Info row 3: High Alch price
		JPanel infoRow3 = new JPanel();
		infoRow3.setLayout(new BoxLayout(infoRow3, BoxLayout.X_AXIS));
		infoRow3.setBackground(ColorScheme.DARK_GRAY_COLOR);
		infoRow3.setBorder(new EmptyBorder(0, 0, 5, 0));
		infoRow3.setAlignmentX(Component.LEFT_ALIGNMENT);

		alchKeyLabel = new JLabel("High Alch: ");
		alchKeyLabel.setForeground(STAT_GRAY);
		alchKeyLabel.setFont(INFO_FONT);

		alchLabel = new JLabel("—");
		alchLabel.setForeground(ALCH_ORANGE);
		alchLabel.setFont(INFO_FONT);

		alchRow = infoRow3;
		infoRow3.add(alchKeyLabel);
		infoRow3.add(alchLabel);
		infoRow3.add(Box.createHorizontalGlue());
		infoRow3.setVisible(false);
		top.add(infoRow3);

		// Quest item button
		questPanel = new JPanel(new BorderLayout());
		questPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		questPanel.setBorder(new EmptyBorder(0, 0, 5, 0));
		questPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		questPanel.setVisible(false);

		JButton questBtn = new JButton("⚠  Quest item — view on wiki");
		questBtn.setFont(INFO_FONT);
		questBtn.setForeground(QUEST_AMBER);
		questBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		questBtn.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(QUEST_AMBER, 1),
			new EmptyBorder(3, 7, 3, 7)
		));
		questBtn.setFocusPainted(false);
		questBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		questBtn.addActionListener(e -> openUrl(currentWikiUrl));
		questPanel.add(questBtn, BorderLayout.WEST);
		top.add(questPanel);

		// Equipment stats wrapper — rebuilt each lookup
		statsWrapper = new JPanel(new BorderLayout());
		statsWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statsWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		statsWrapper.setVisible(false);
		top.add(statsWrapper);

		add(top, BorderLayout.NORTH);

		// ── CENTER ────────────────────────────────────────────────────────
		extractArea = new JTextArea();
		extractArea.setWrapStyleWord(true);
		extractArea.setLineWrap(true);
		extractArea.setEditable(false);
		extractArea.setFocusable(false);
		extractArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		extractArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		extractArea.setFont(new Font(Font.DIALOG, Font.PLAIN, config.fontSize()));
		extractArea.setBorder(new EmptyBorder(6, 6, 6, 6));
		extractArea.setText("Right-click any item in your inventory\nand choose \"Wiki Lookup\".");

		JScrollPane scroll = new JScrollPane(extractArea);
		scroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		scroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		add(scroll, BorderLayout.CENTER);

		// ── FOOTER ────────────────────────────────────────────────────────
		JPanel footer = new JPanel(new BorderLayout(0, 4));
		footer.setBackground(ColorScheme.DARK_GRAY_COLOR);
		footer.setBorder(new EmptyBorder(6, 0, 0, 0));

		statusLabel = new JLabel(" ");
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setFont(STAT_FONT);

		openWikiButton = new JButton("Open Full Article in Browser");
		openWikiButton.setBackground(new Color(50, 100, 200));
		openWikiButton.setForeground(Color.WHITE);
		openWikiButton.setFocusPainted(false);
		openWikiButton.setBorderPainted(false);
		openWikiButton.setEnabled(false);
		openWikiButton.addActionListener(e -> openUrl(currentWikiUrl));

		footer.add(statusLabel, BorderLayout.NORTH);
		footer.add(openWikiButton, BorderLayout.SOUTH);
		add(footer, BorderLayout.SOUTH);
	}

	// ── Public API ────────────────────────────────────────────────────────────

	public void setLoading(String name, BufferedImage icon)
	{
		titleLabel.setText(name);
		iconLabel.setIcon(icon != null ? new ImageIcon(icon) : null);
		membersLabel.setText("—");
		membersLabel.setForeground(STAT_GRAY);
		geKeyLabel.setText("Grand Exchange: ");
		gePriceLabel.setText("loading...");
		gePriceLabel.setForeground(STAT_GRAY);
		alchRow.setVisible(false);
		questPanel.setVisible(false);
		statsWrapper.setVisible(false);
		extractArea.setText("Loading wiki article...");
		extractArea.setCaretPosition(0);
		openWikiButton.setEnabled(false);
		statusLabel.setText(" ");
		currentWikiUrl = null;
	}

	public void setContent(WikiItemInfo info)
	{
		currentWikiUrl = info.wikiUrl;
		titleLabel.setText(info.title);

		if (info.icon != null)
		{
			iconLabel.setIcon(new ImageIcon(info.icon));
		}

		// Members / Free to Play
		if (info.members)
		{
			membersLabel.setText("Members");
			membersLabel.setForeground(MEMBERS_GREEN);
		}
		else
		{
			membersLabel.setText("Free to Play");
			membersLabel.setForeground(FTP_YELLOW);
		}

		// Grand Exchange price
		geKeyLabel.setText("Grand Exchange: ");
		if (info.gePrice > 0)
		{
			gePriceLabel.setText(formatGp(info.gePrice));
			gePriceLabel.setForeground(GOLD);
		}
		else
		{
			gePriceLabel.setText("N/A");
			gePriceLabel.setForeground(NEGATIVE);
		}

		// High Alch price
		if (info.highAlch > 0)
		{
			alchLabel.setText(formatGp(info.highAlch));
			alchRow.setVisible(true);
		}
		else
		{
			alchRow.setVisible(false);
		}

		questPanel.setVisible(info.questItem);
		buildStatsPanel(info);

		extractArea.setFont(new Font(Font.DIALOG, Font.PLAIN, config.fontSize()));
		String text = info.extract;
		if (text.length() > MAX_EXTRACT)
		{
			text = text.substring(0, MAX_EXTRACT).trim()
				+ "...\n\n(Open the full article for more.)";
		}
		extractArea.setText(text);
		extractArea.setCaretPosition(0);

		openWikiButton.setEnabled(true);
		statusLabel.setText(" ");
	}

	public void setError(String message)
	{
		titleLabel.setText("Not found");
		iconLabel.setIcon(null);
		membersLabel.setText("—");
		membersLabel.setForeground(STAT_GRAY);
		geKeyLabel.setText("Grand Exchange: ");
		gePriceLabel.setText("—");
		gePriceLabel.setForeground(STAT_GRAY);
		alchRow.setVisible(false);
		questPanel.setVisible(false);
		statsWrapper.setVisible(false);
		extractArea.setText(message);
		extractArea.setCaretPosition(0);
		openWikiButton.setEnabled(false);
		statusLabel.setText("Could not load wiki data");
		currentWikiUrl = null;
	}

	/** Called by plugin on config change so font updates without re-lookup. */
	public void refreshFont()
	{
		extractArea.setFont(new Font(Font.DIALOG, Font.PLAIN, config.fontSize()));
		extractArea.revalidate();
		extractArea.repaint();
	}

	// ── Equipment stats ───────────────────────────────────────────────────────

	private static class StatEntry
	{
		final String name;
		final int newVal;
		final int oldVal;
		final boolean hasOld;

		StatEntry(String name, int newVal, int oldVal, boolean hasOld)
		{
			this.name = name;
			this.newVal = newVal;
			this.oldVal = oldVal;
			this.hasOld = hasOld;
		}
	}

	private void buildStatsPanel(WikiItemInfo info)
	{
		statsWrapper.removeAll();

		if (info.itemStats == null || info.itemStats.getEquipment() == null)
		{
			statsWrapper.setVisible(false);
			statsWrapper.revalidate();
			return;
		}

		ItemEquipmentStats newEq = info.itemStats.getEquipment();
		ItemEquipmentStats oldEq = (info.equippedStats != null)
			? info.equippedStats.getEquipment() : null;
		boolean hasOld = oldEq != null;

		// Collect non-zero rows
		List<StatEntry> entries = new ArrayList<>();
		addEntry(entries, "Stab Atk",  newEq.getAstab(),        hasOld ? oldEq.getAstab()        : 0, hasOld);
		addEntry(entries, "Slash Atk", newEq.getAslash(),       hasOld ? oldEq.getAslash()       : 0, hasOld);
		addEntry(entries, "Crush Atk", newEq.getAcrush(),       hasOld ? oldEq.getAcrush()       : 0, hasOld);
		addEntry(entries, "Magic Atk", newEq.getAmagic(),       hasOld ? oldEq.getAmagic()       : 0, hasOld);
		addEntry(entries, "Range Atk", newEq.getArange(),       hasOld ? oldEq.getArange()       : 0, hasOld);
		addEntry(entries, "Stab Def",  newEq.getDstab(),        hasOld ? oldEq.getDstab()        : 0, hasOld);
		addEntry(entries, "Slash Def", newEq.getDslash(),       hasOld ? oldEq.getDslash()       : 0, hasOld);
		addEntry(entries, "Crush Def", newEq.getDcrush(),       hasOld ? oldEq.getDcrush()       : 0, hasOld);
		addEntry(entries, "Magic Def", newEq.getDmagic(),       hasOld ? oldEq.getDmagic()       : 0, hasOld);
		addEntry(entries, "Range Def", newEq.getDrange(),       hasOld ? oldEq.getDrange()       : 0, hasOld);
		addEntry(entries, "Strength",  newEq.getStr(),          hasOld ? oldEq.getStr()          : 0, hasOld);
		addEntry(entries, "Range Str", newEq.getRstr(),         hasOld ? oldEq.getRstr()         : 0, hasOld);
		addEntry(entries, "Magic Dmg", (int) newEq.getMdmg(),   hasOld ? (int) oldEq.getMdmg()   : 0, hasOld);
		addEntry(entries, "Prayer",    newEq.getPrayer(),       hasOld ? oldEq.getPrayer()       : 0, hasOld);

		if (entries.isEmpty())
		{
			statsWrapper.setVisible(false);
			statsWrapper.revalidate();
			return;
		}

		// Section header — HTML for two-tone text
		String headerHtml = "<html><span style='color:#a0a0a0'>Equipment Bonuses</span>";
		if (info.equippedItemName != null)
		{
			headerHtml += "<span style='color:#a0a0a0'>  vs  </span>"
				+ "<span style='color:#a0d2ff'>" + info.equippedItemName + "</span>";
		}
		headerHtml += "</html>";
		JLabel headerLabel = new JLabel(headerHtml);
		headerLabel.setFont(INFO_FONT);
		headerLabel.setBorder(new EmptyBorder(2, 0, 4, 0));
		statsWrapper.add(headerLabel, BorderLayout.NORTH);

		// Build table model
		String[] colNames = hasOld
			? new String[]{"Stat", "Value", "Change"}
			: new String[]{"Stat", "Value"};

		DefaultTableModel model = new DefaultTableModel(colNames, 0)
		{
			@Override
			public boolean isCellEditable(int row, int col)
			{
				return false;
			}
		};

		for (StatEntry e : entries)
		{
			String sign = e.newVal >= 0 ? "+" : "";
			String valStr = sign + e.newVal;
			if (hasOld)
			{
				int diff = e.newVal - e.oldVal;
				String diffStr = (diff >= 0 ? "+" : "") + diff;
				model.addRow(new Object[]{e.name, valStr, diffStr});
			}
			else
			{
				model.addRow(new Object[]{e.name, valStr});
			}
		}

		JTable table = new JTable(model);
		table.setFont(INFO_FONT);
		table.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		table.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		table.setGridColor(ColorScheme.MEDIUM_GRAY_COLOR);
		table.setRowHeight(INFO_FONT.getSize() + 6);
		table.setShowGrid(true);
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);

		// Header styling
		table.getTableHeader().setFont(INFO_FONT);
		table.getTableHeader().setForeground(STAT_GRAY);
		table.getTableHeader().setBackground(ColorScheme.DARK_GRAY_COLOR);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);

		// Column widths
		table.getColumnModel().getColumn(0).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setPreferredWidth(50);
		if (hasOld)
		{
			table.getColumnModel().getColumn(2).setPreferredWidth(55);
		}

		// Stat name column — left-aligned, gray
		DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
				JTable t, Object value, boolean sel, boolean focus, int row, int col)
			{
				super.getTableCellRendererComponent(t, value, sel, focus, row, col);
				setForeground(STAT_GRAY);
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
				setFont(INFO_FONT);
				setHorizontalAlignment(SwingConstants.LEFT);
				setBorder(new EmptyBorder(0, 4, 0, 4));
				return this;
			}
		};
		table.getColumnModel().getColumn(0).setCellRenderer(nameRenderer);

		// Value column — right-aligned, light gray
		DefaultTableCellRenderer valueRenderer = new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(
				JTable t, Object value, boolean sel, boolean focus, int row, int col)
			{
				super.getTableCellRendererComponent(t, value, sel, focus, row, col);
				setForeground(ColorScheme.LIGHT_GRAY_COLOR);
				setBackground(ColorScheme.DARKER_GRAY_COLOR);
				setFont(INFO_FONT);
				setHorizontalAlignment(SwingConstants.RIGHT);
				setBorder(new EmptyBorder(0, 4, 0, 4));
				return this;
			}
		};
		table.getColumnModel().getColumn(1).setCellRenderer(valueRenderer);

		// Change column — color-coded green/red/gray
		if (hasOld)
		{
			DefaultTableCellRenderer changeRenderer = new DefaultTableCellRenderer()
			{
				@Override
				public Component getTableCellRendererComponent(
					JTable t, Object value, boolean sel, boolean focus, int row, int col)
				{
					super.getTableCellRendererComponent(t, value, sel, focus, row, col);
					setBackground(ColorScheme.DARKER_GRAY_COLOR);
					setFont(INFO_FONT);
					setHorizontalAlignment(SwingConstants.RIGHT);
					setBorder(new EmptyBorder(0, 4, 0, 4));

					String str = value != null ? value.toString() : "0";
					try
					{
						int diff = Integer.parseInt(str.replace("+", ""));
						setForeground(diff > 0 ? POSITIVE : diff < 0 ? NEGATIVE : STAT_GRAY);
					}
					catch (NumberFormatException ex)
					{
						setForeground(STAT_GRAY);
					}
					return this;
				}
			};
			table.getColumnModel().getColumn(2).setCellRenderer(changeRenderer);
		}

		// Size the scroll pane exactly to the table content — no scrollbars
		int tableHeight = table.getRowHeight() * entries.size()
			+ table.getTableHeader().getPreferredSize().height;
		JScrollPane tableScroll = new JScrollPane(table,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		tableScroll.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
		tableScroll.setPreferredSize(new Dimension(0, tableHeight));
		tableScroll.setMinimumSize(new Dimension(0, tableHeight));
		tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, tableHeight));
		tableScroll.getViewport().setBackground(ColorScheme.DARKER_GRAY_COLOR);

		statsWrapper.add(tableScroll, BorderLayout.CENTER);
		statsWrapper.setBorder(new EmptyBorder(0, 0, 6, 0));
		statsWrapper.setVisible(true);
		statsWrapper.revalidate();
		statsWrapper.repaint();
	}

	private static void addEntry(List<StatEntry> list, String name,
		int newVal, int oldVal, boolean hasOld)
	{
		if (newVal == 0 && (!hasOld || oldVal == 0))
		{
			return;
		}
		list.add(new StatEntry(name, newVal, oldVal, hasOld));
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static String formatGp(int gp)
	{
		if (gp >= 1_000_000)
		{
			return String.format("%.1fM gp", gp / 1_000_000.0);
		}
		if (gp >= 1_000)
		{
			return String.format("%.1fK gp", gp / 1_000.0);
		}
		return NumberFormat.getInstance().format(gp) + " gp";
	}

	private void openUrl(String url)
	{
		if (url == null)
		{
			return;
		}
		LinkBrowser.browse(url);
	}
}
