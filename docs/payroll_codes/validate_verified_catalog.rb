#!/usr/bin/env ruby

require "csv"

catalog_path = File.join(__dir__, "payroll_codes_verified.csv")
rows = CSV.read(catalog_path, headers: true)
required_headers = %w[source type code short_name description status]

abort "Unexpected headers: #{rows.headers.inspect}" unless rows.headers == required_headers
abort "Expected 286 rows, got #{rows.size}" unless rows.size == 286

expected_source_counts = {
  "0336" => 35,
  "0337" => 27,
  "0338" => 5,
  "0339" => 75,
  "0340" => 58,
  "0341" => 75,
  "USER" => 1,
  "0342" => 10,
}
actual_source_counts = rows.group_by { |row| row["source"] }.transform_values(&:size)
abort "Unexpected source counts: #{actual_source_counts.inspect}" unless actual_source_counts == expected_source_counts

rows.each_with_index do |row, index|
  line = index + 2
  required_headers.each do |header|
    abort "Empty #{header} at line #{line}" if row[header].to_s.strip.empty?
  end
  abort "Invalid code #{row['code'].inspect} at line #{line}" unless row["code"].match?(/\A[0-9A-Z]+\z/)
  abort "Invalid type at line #{line}" unless %w[ACCRUAL DEDUCTION].include?(row["type"])
  abort "Unconfirmed row at line #{line}" unless row["status"] == "CONFIRMED"
end

duplicates = rows.group_by { |row| row["code"] }.select { |_, values| values.size > 1 }
expected_duplicates = { "035L" => 2 }
actual_duplicates = duplicates.transform_values(&:size)
abort "Unexpected duplicate codes: #{actual_duplicates.inspect}" unless actual_duplicates == expected_duplicates

source_duplicates = rows.group_by { |row| [row["source"], row["code"]] }.select { |_, values| values.size > 1 }
abort "Duplicate code within a source: #{source_duplicates.keys.inspect}" unless source_duplicates.empty?

puts "Verified payroll catalog: #{rows.size} rows; all checks passed"
