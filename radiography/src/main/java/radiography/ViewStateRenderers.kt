package radiography

import android.content.res.Resources.NotFoundException
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import radiography.StateRenderer.Companion.stateRendererFor

object ViewStateRenderers {

  val viewRenderer: StateRenderer<View> = stateRendererFor { view ->
    if (view.id != View.NO_ID && view.resources != null) {
      try {
        val resourceName = view.resources.getResourceEntryName(view.id)
        append("id:$resourceName")
      } catch (ignore: NotFoundException) {
        // Do nothing.
      }
    }

    when (view.visibility) {
      View.GONE -> append("GONE")
      View.INVISIBLE -> append("INVISIBLE")
    }

    append("${view.width}x${view.height}px")

    if (view.isFocused) {
      append("focused")
    }

    if (!view.isEnabled) {
      append("disabled")
    }

    if (view.isSelected) {
      append("selected")
    }
  }

  val checkableRenderer: StateRenderer<Checkable> = stateRendererFor { checkable ->
    if (checkable.isChecked) {
      append("checked")
    }
  }

  val defaultsNoPii: List<StateRenderer<*>> = listOf(
      viewRenderer,
      textViewRenderer(includeTextViewText = false, textViewTextMaxLength = 0),
      checkableRenderer
  )

  val defaultsIncludingPii: List<StateRenderer<*>> = listOf(
      viewRenderer,
      textViewRenderer(includeTextViewText = true),
      checkableRenderer
  )

  /**
   * @param includeTextViewText whether to include the string content of TextView instances in
   * the rendered view hierarchy. Defaults to false to avoid including any PII.
   *
   * @param textViewTextMaxLength the max size of the string content of TextView instances when
   * [includeTextViewText] is true. When the max size is reached, the text is trimmed to
   * a [textViewTextMaxLength] - 1 length and ellipsized with a '…' character.
   */
  fun textViewRenderer(
    includeTextViewText: Boolean = false,
    textViewTextMaxLength: Int = Int.MAX_VALUE
  ): StateRenderer<TextView> {
    if (includeTextViewText) {
      check(textViewTextMaxLength >= 0) {
        "textFieldMaxLength should be greater than 0, not $textViewTextMaxLength"
      }
    }
    return stateRendererFor { textView ->
      var text = textView.text
      if (text != null) {
        append("text-length:${text.length}")
        if (includeTextViewText) {
          if (text.length > textViewTextMaxLength) {
            text = "${text.subSequence(0, textViewTextMaxLength - 1)}…"
          }
          append("text:\"$text\"")
        }
      }
      if (textView.isInputMethodTarget) {
        append("ime-target")
      }
    }
  }
}
