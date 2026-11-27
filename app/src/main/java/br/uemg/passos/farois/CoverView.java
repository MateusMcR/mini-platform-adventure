package br.uemg.passos.farois;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

/** A ilustração acompanha a altura da tela e mantém o farol à vista em telas largas. */
public final class CoverView extends View {
  private final Bitmap coast;
  private final Paint pixels = new Paint();
  private final RectF area = new RectF();

  public CoverView(Context context, AttributeSet attrs) {
    super(context, attrs);
    coast = BitmapFactory.decodeResource(getResources(), R.drawable.porto);
    pixels.setFilterBitmap(false);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    float scale =
        Math.max((float) getHeight() / coast.getHeight(), (float) getWidth() / coast.getWidth());
    float width = coast.getWidth() * scale, height = coast.getHeight() * scale;
    area.set(
        getWidth() - width, (getHeight() - height) * .5f, getWidth(), (getHeight() + height) * .5f);
    canvas.drawBitmap(coast, null, area, pixels);
  }
}
