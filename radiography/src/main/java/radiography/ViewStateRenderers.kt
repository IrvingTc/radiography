package radiography

import android.annotation.SuppressLint
import android.content.res.Resources.NotFoundException
import android.view.View
import android.widget.Checkable
import android.widget.TextView
import radiography.ScannableView.AndroidView
import radiography.compose.ComposeLayoutRenderers
import radiography.compose.ExperimentalRadiographyComposeApi

@OptIn(ExperimentalRadiographyComposeApi::class)
public object ViewStateRenderers {

  @JvmField
  public val ViewRenderer: ViewStateRenderer = androidViewStateRendererFor<View> { view ->
    if (view.id != View.NO_ID && view.resources != null) {
      try {
        val resourceName = view.resources.getResourceEntryName(view.id)
        append("id:$resourceName")
      } catch (ignore: NotFoundException) {
        // Do nothing.
      }
    }

    @SuppressLint("SwitchIntDef")
    when (view.visibility) {
      View.GONE -> append("GONE")
      View.INVISIBLE -> append("INVISIBLE")
    }

    append(formatPixelDimensions(view.width, view.height))

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

  @JvmField
  public val CheckableRenderer: ViewStateRenderer =
    androidViewStateRendererFor<Checkable> { checkable ->
      if (checkable.isChecked) {
        append("checked")
      }
    }

  @JvmField
  public val DefaultsNoPii: List<ViewStateRenderer> = listOf(
      ViewRenderer,
      textViewRenderer(includeTextViewText = false, textViewTextMaxLength = 0),
      CheckableRenderer,
  ) + ComposeLayoutRenderers.DefaultsNoPii

  @JvmField
  public val DefaultsIncludingPii: List<ViewStateRenderer> = listOf(
      ViewRenderer,
      textViewRenderer(includeTextViewText = true),
      CheckableRenderer,
  ) + ComposeLayoutRenderers.DefaultsIncludingPii

  /**
   * @param includeTextViewText whether to include the string content of TextView instances in
   * the rendered view hierarchy. Defaults to false to avoid including any PII.
   *
   * @param textViewTextMaxLength the max size of the string content of TextView instances when
   * [includeTextViewText] is true. When the max size is reached, the text is trimmed to
   * a [textViewTextMaxLength] - 1 length and ellipsized with a '…' character.
   */
  @JvmStatic
  @JvmOverloads
  public fun textViewRenderer(
    includeTextViewText: Boolean = false,
    textViewTextMaxLength: Int = Int.MAX_VALUE
  ): ViewStateRenderer {
    if (includeTextViewText) {
      check(textViewTextMaxLength >= 0) {
        "textFieldMaxLength should be greater than 0, not $textViewTextMaxLength"
      }
    }
    return androidViewStateRendererFor<TextView> { textView ->
      var text = textView.text
      if (text != null) {
        append("text-length:${text.length}")
        if (includeTextViewText) {
          text = text.ellipsize(textViewTextMaxLength)
          append("text:\"$text\"")
        }
      }
      if (textView.isInputMethodTarget) {
        append("ime-target")
      }
    }
  }

  /**
   * Creates a [ViewStateRenderer] that renders [AndroidView]s with views of type [T].
   */
  // This function is only visible to Kotlin consumers of this library.
  public inline fun <reified T : Any> androidViewStateRendererFor(
    noinline renderer: AttributeAppendable.(T) -> Unit
  ): ViewStateRenderer {
    // Don't create an anonymous instance of ViewStateRenderer here, since that would generate a new
    // anonymous class at every call site.
    return androidViewStateRendererFor(T::class.java, renderer)
  }

  /**
   * Creates a [ViewStateRenderer] that renders [AndroidView]s with views of type [T].
   */
  // This function is only visible to Java consumers of this library.
  @JvmStatic
  @PublishedApi internal fun <T : Any> androidViewStateRendererFor(
    renderedClass: Class<T>,
    renderer: AttributeAppendable.(T) -> Unit
  ): ViewStateRenderer = ViewStateRenderer { scannableView ->
    val view = (scannableView as? AndroidView)
        ?.view
        ?: return@ViewStateRenderer
    if (!renderedClass.isInstance(view)) return@ViewStateRenderer
    @Suppress("UNCHECKED_CAST")
    renderer(view as T)
  }
}
