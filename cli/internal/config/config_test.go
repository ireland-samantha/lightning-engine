// Package config provides configuration management for the Lightning CLI.
package config

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/spf13/viper"
)

func TestDefaultValues(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.ControlPlaneURL != DefaultControlPlaneURL {
		t.Errorf("expected default ControlPlaneURL %s, got %s", DefaultControlPlaneURL, cfg.ControlPlaneURL)
	}

	if cfg.OutputFormat != "table" {
		t.Errorf("expected default OutputFormat 'table', got %s", cfg.OutputFormat)
	}
}

func TestLoadFromEnvironment(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	// Set environment variables (viper uses LIGHTNING_ prefix and underscore keys)
	os.Setenv("LIGHTNING_CONTROL_PLANE_URL", "http://env.example.com:8080")
	os.Setenv("LIGHTNING_OUTPUT_FORMAT", "json")
	defer func() {
		os.Unsetenv("LIGHTNING_CONTROL_PLANE_URL")
		os.Unsetenv("LIGHTNING_OUTPUT_FORMAT")
	}()

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.ControlPlaneURL != "http://env.example.com:8080" {
		t.Errorf("expected ControlPlaneURL from env, got %s", cfg.ControlPlaneURL)
	}

	if cfg.OutputFormat != "json" {
		t.Errorf("expected OutputFormat 'json', got %s", cfg.OutputFormat)
	}
}

func TestLoadFromFile(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	// Create a temp directory
	tmpDir := t.TempDir()

	// Create config file
	configContent := `control_plane_url: http://file.example.com:8080
auth_token: file-token
output_format: yaml
`
	configPath := filepath.Join(tmpDir, ".lightning.yaml")
	if err := os.WriteFile(configPath, []byte(configContent), 0644); err != nil {
		t.Fatalf("failed to write config file: %v", err)
	}

	// Change to temp directory
	oldWd, _ := os.Getwd()
	os.Chdir(tmpDir)
	defer os.Chdir(oldWd)

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if cfg.ControlPlaneURL != "http://file.example.com:8080" {
		t.Errorf("expected ControlPlaneURL from file, got %s", cfg.ControlPlaneURL)
	}

	if cfg.AuthToken != "file-token" {
		t.Errorf("expected AuthToken from file, got %s", cfg.AuthToken)
	}

	if cfg.OutputFormat != "yaml" {
		t.Errorf("expected OutputFormat 'yaml', got %s", cfg.OutputFormat)
	}
}

func TestSaveAndLoad(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	// Create a temp home directory
	tmpHome := t.TempDir()

	// Override home directory
	oldHome := os.Getenv("HOME")
	os.Setenv("HOME", tmpHome)
	defer os.Setenv("HOME", oldHome)

	// Create and save config
	cfg := &Config{
		ControlPlaneURL: "http://saved.example.com:8080",
		AuthToken:       "saved-token",
		OutputFormat:    "json",
	}

	if err := Save(cfg); err != nil {
		t.Fatalf("failed to save config: %v", err)
	}

	// Verify file was created
	configPath := filepath.Join(tmpHome, ".lightning.yaml")
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		t.Error("config file was not created")
	}

	// Reset viper and reload
	viper.Reset()
	viper.AddConfigPath(tmpHome)

	loaded, err := Load()
	if err != nil {
		t.Fatalf("failed to load config: %v", err)
	}

	if loaded.ControlPlaneURL != cfg.ControlPlaneURL {
		t.Errorf("expected ControlPlaneURL %s, got %s", cfg.ControlPlaneURL, loaded.ControlPlaneURL)
	}

	if loaded.AuthToken != cfg.AuthToken {
		t.Errorf("expected AuthToken %s, got %s", cfg.AuthToken, loaded.AuthToken)
	}

	if loaded.OutputFormat != cfg.OutputFormat {
		t.Errorf("expected OutputFormat %s, got %s", cfg.OutputFormat, loaded.OutputFormat)
	}
}

func TestGetters(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	// Set values
	viper.Set("control_plane_url", "http://getter.example.com")
	viper.Set("auth_token", "getter-token")
	viper.Set("output_format", "yaml")

	if GetControlPlaneURL() != "http://getter.example.com" {
		t.Errorf("expected 'http://getter.example.com', got %s", GetControlPlaneURL())
	}

	if GetAuthToken() != "getter-token" {
		t.Errorf("expected 'getter-token', got %s", GetAuthToken())
	}

	if GetOutputFormat() != "yaml" {
		t.Errorf("expected 'yaml', got %s", GetOutputFormat())
	}
}

func TestSetters(t *testing.T) {
	// Reset viper for testing
	viper.Reset()

	SetControlPlaneURL("http://setter.example.com")
	SetAuthToken("setter-token")
	SetOutputFormat("json")

	if GetControlPlaneURL() != "http://setter.example.com" {
		t.Errorf("expected 'http://setter.example.com', got %s", GetControlPlaneURL())
	}

	if GetAuthToken() != "setter-token" {
		t.Errorf("expected 'setter-token', got %s", GetAuthToken())
	}

	if GetOutputFormat() != "json" {
		t.Errorf("expected 'json', got %s", GetOutputFormat())
	}
}

func TestConfigStruct(t *testing.T) {
	cfg := Config{
		ControlPlaneURL: "http://test.example.com",
		AuthToken:       "test-token",
		OutputFormat:    "table",
	}

	if cfg.ControlPlaneURL != "http://test.example.com" {
		t.Errorf("expected ControlPlaneURL 'http://test.example.com', got %s", cfg.ControlPlaneURL)
	}

	if cfg.AuthToken != "test-token" {
		t.Errorf("expected AuthToken 'test-token', got %s", cfg.AuthToken)
	}

	if cfg.OutputFormat != "table" {
		t.Errorf("expected OutputFormat 'table', got %s", cfg.OutputFormat)
	}
}

func TestConstants(t *testing.T) {
	if DefaultControlPlaneURL != "http://localhost:8081" {
		t.Errorf("expected DefaultControlPlaneURL 'http://localhost:8081', got %s", DefaultControlPlaneURL)
	}

	if ConfigFileName != ".lightning" {
		t.Errorf("expected ConfigFileName '.lightning', got %s", ConfigFileName)
	}

	if ConfigFileType != "yaml" {
		t.Errorf("expected ConfigFileType 'yaml', got %s", ConfigFileType)
	}
}
