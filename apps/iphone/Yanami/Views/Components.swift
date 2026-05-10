import SwiftUI

struct MetricView: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value)
                .font(.headline)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct StatusPill: View {
    let isOnline: Bool

    var body: some View {
        Text(isOnline ? "Online" : "Offline")
            .font(.caption2.bold())
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(isOnline ? Color.green.opacity(0.16) : Color.red.opacity(0.16))
            .foregroundStyle(isOnline ? .green : .red)
            .clipShape(Capsule())
    }
}

struct ResourceMeter: View {
    let title: String
    let value: Double
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                Spacer()
                Text(label)
                    .foregroundStyle(.secondary)
            }
            .font(.caption)
            ProgressView(value: min(max(value, 0), 1))
        }
    }
}

struct DetailLine: View {
    let title: String
    let value: String

    init(_ title: String, _ value: String) {
        self.title = title
        self.value = value
    }

    var body: some View {
        if !value.isEmpty {
            HStack(alignment: .top) {
                Text(title)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(value)
                    .multilineTextAlignment(.trailing)
            }
        }
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.caption)
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(isSelected ? Color.accentColor.opacity(0.18) : Color.secondary.opacity(0.12))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

struct EmptyStateView: View {
    let title: String
    let systemImage: String
    let message: String

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: systemImage)
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(title)
                .font(.headline)
            Text(message)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 24)
    }
}
