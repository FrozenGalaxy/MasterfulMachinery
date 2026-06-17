package io.ticticboom.mods.mm.client.structure;

import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class GuiStructureLayout {
    private final StructureLayout layout;
    private final BlockPos controllerPos;

    @Getter
    private List<GuiCountedItemStack> countedKeys = new ArrayList<>();

    public GuiStructureLayout(StructureLayout layout) {
        this.layout = layout;
        controllerPos = new BlockPos(0, 0, 0);
    }

    public List<PositionedCyclingBlockRenderer> createBlockRenderers() {
        var result = new ArrayList<PositionedCyclingBlockRenderer>();
        for (PositionedLayoutPiece piece : layout.getPositionedPieces()) {
            GuiStructureLayoutPiece guiPiece = piece.piece().guiPiece();
            var renderers = guiPiece.createBlockRenderer(piece.pos());
            // skip pieces that have no renderer blocks (e.g. port types with no matching registered blocks at the configured tier)
            if (renderers == null || renderers.isEmpty()) continue;
            result.add(new PositionedCyclingBlockRenderer(renderers, piece.pos()));
        }
        return result;
    }
}
