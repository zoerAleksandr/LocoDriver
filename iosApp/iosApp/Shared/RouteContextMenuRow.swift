import SwiftUI
import UIKit

// MARK: - Модель пункта контекстного меню

struct ContextMenuAction {
    let title: String
    let systemImage: String
    var isDestructive: Bool = false
    let action: () -> Void
}

// MARK: - SwiftUI-обёртка строки с UIKit-контекстным меню

/// Строка списка с UIKit `UIContextMenuInteraction` вместо SwiftUI
/// `.contextMenu`. В отличие от SwiftUI, поддерживает `preferredCommitStyle = .pop`:
/// тап по preview закрывает меню и коммитит действие (открыть маршрут).
///
/// Обычный тап по строке также вызывает `onCommit` — чтобы поведение
/// совпадало с NavigationLink.
struct RouteContextMenuRow<Content: View, Preview: View>: UIViewRepresentable {

    let content: Content
    let preview: () -> Preview
    let actions: () -> [ContextMenuAction]
    let onCommit: () -> Void

    func makeUIView(context: Context) -> ContextMenuHostView<Content, Preview> {
        ContextMenuHostView(
            content: content,
            preview: preview,
            actions: actions,
            onCommit: onCommit
        )
    }

    func updateUIView(_ uiView: ContextMenuHostView<Content, Preview>, context: Context) {
        uiView.update(
            content: content,
            preview: preview,
            actions: actions,
            onCommit: onCommit
        )
    }

    @available(iOS 16.0, *)
    func sizeThatFits(_ proposal: ProposedViewSize,
                      uiView: ContextMenuHostView<Content, Preview>,
                      context: Context) -> CGSize? {
        let w = proposal.width ?? UIView.noIntrinsicMetric
        let size = uiView.hosting.sizeThatFits(
            in: CGSize(width: w, height: UIView.layoutFittingCompressedSize.height)
        )
        return size
    }
}

// MARK: - UIView-хост

final class ContextMenuHostView<Content: View, Preview: View>: UIView, UIContextMenuInteractionDelegate {

    fileprivate let hosting: UIHostingController<Content>
    private var previewBuilder: () -> Preview
    private var actionsBuilder: () -> [ContextMenuAction]
    private var onCommit: () -> Void

    init(content: Content,
         preview: @escaping () -> Preview,
         actions: @escaping () -> [ContextMenuAction],
         onCommit: @escaping () -> Void) {
        self.hosting = UIHostingController(rootView: content)
        self.previewBuilder = preview
        self.actionsBuilder = actions
        self.onCommit = onCommit
        super.init(frame: .zero)

        backgroundColor = .clear
        hosting.view.backgroundColor = .clear
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(hosting.view)
        NSLayoutConstraint.activate([
            hosting.view.topAnchor.constraint(equalTo: topAnchor),
            hosting.view.leadingAnchor.constraint(equalTo: leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: trailingAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: bottomAnchor)
        ])

        // Обычный тап — тот же коммит, что и тап по preview.
        let tap = UITapGestureRecognizer(target: self, action: #selector(handleTap))
        tap.cancelsTouchesInView = false
        addGestureRecognizer(tap)

        // Long-press → контекстное меню.
        let interaction = UIContextMenuInteraction(delegate: self)
        addInteraction(interaction)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) not implemented") }

    func update(content: Content,
                preview: @escaping () -> Preview,
                actions: @escaping () -> [ContextMenuAction],
                onCommit: @escaping () -> Void) {
        hosting.rootView = content
        self.previewBuilder = preview
        self.actionsBuilder = actions
        self.onCommit = onCommit
        invalidateIntrinsicContentSize()
    }

    override var intrinsicContentSize: CGSize {
        hosting.sizeThatFits(in: CGSize(
            width: bounds.width > 0 ? bounds.width : UIView.layoutFittingCompressedSize.width,
            height: UIView.layoutFittingCompressedSize.height
        ))
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        invalidateIntrinsicContentSize()
    }

    @objc private func handleTap() {
        onCommit()
    }

    // MARK: UIContextMenuInteractionDelegate

    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        configurationForMenuAtLocation location: CGPoint
    ) -> UIContextMenuConfiguration? {
        let previewBuilder = self.previewBuilder
        let actionsBuilder = self.actionsBuilder
        return UIContextMenuConfiguration(
            identifier: nil,
            previewProvider: {
                let host = UIHostingController(rootView: previewBuilder())
                host.view.backgroundColor = .clear
                let size = host.sizeThatFits(in: CGSize(
                    width: 360,
                    height: UIView.layoutFittingCompressedSize.height
                ))
                host.preferredContentSize = CGSize(width: 360, height: size.height)
                return host
            },
            actionProvider: { _ in
                let actions = actionsBuilder().map { ma -> UIAction in
                    UIAction(
                        title: ma.title,
                        image: UIImage(systemName: ma.systemImage),
                        attributes: ma.isDestructive ? .destructive : []
                    ) { _ in ma.action() }
                }
                return UIMenu(title: "", children: actions)
            }
        )
    }

    func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        willPerformPreviewActionForMenuWith configuration: UIContextMenuConfiguration,
        animator: any UIContextMenuInteractionCommitAnimating
    ) {
        animator.preferredCommitStyle = .pop
        let onCommit = self.onCommit
        animator.addCompletion {
            onCommit()
        }
    }
}
