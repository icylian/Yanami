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

struct CircularUsageIndicator: View {
    let label: String
    let percent: Double
    let detail: String
    var ringSize: CGFloat = 64
    var strokeWidth: CGFloat = 5

    var body: some View {
        VStack(spacing: 4) {
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.2), lineWidth: strokeWidth)
                
                Circle()
                    .trim(from: 0, to: CGFloat(min(max(percent / 100, 0), 1)))
                    .stroke(usageColor, style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.easeOut(duration: 0.6), value: percent)
                
                VStack(spacing: 1) {
                    Text(label)
                        .font(.system(size: 8, weight: .medium))
                        .foregroundStyle(.secondary)
                    Text("\(Int(percent))%")
                        .font(.system(size: 12, weight: .bold))
                }
            }
            .frame(width: ringSize, height: ringSize)
            
            Text(detail)
                .font(.system(size: 9))
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
    }

    private var usageColor: Color {
        if percent >= 90 { return .red }
        if percent >= 70 { return .orange }
        return .accentColor
    }
}

struct UptimeBadge: View {
    let uptime: Int64
    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "clock")
            Text(Formatters.uptime(uptime))
        }
        .font(.system(size: 10, weight: .medium))
        .padding(.horizontal, 6)
        .padding(.vertical, 3)
        .background(Color.blue.opacity(0.12))
        .foregroundStyle(.blue)
        .clipShape(Capsule())
    }
}

struct ExpiryBadge: View {
    let dateString: String
    var body: some View {
        Text(dateString)
            .font(.system(size: 10, weight: .medium))
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(Color.orange.opacity(0.12))
            .foregroundStyle(.orange)
            .clipShape(Capsule())
    }
}

struct StatusBadge: View {
    let isOnline: Bool
    var body: some View {
        Text(isOnline ? "Online" : "Offline")
            .font(.system(size: 10, weight: .bold))
            .padding(.horizontal, 6)
            .padding(.vertical, 3)
            .background(isOnline ? Color.green.opacity(0.12) : Color.red.opacity(0.12))
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
