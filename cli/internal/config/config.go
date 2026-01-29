// Package config provides configuration management for the Lightning CLI.
package config

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/viper"
)

const (
	// DefaultControlPlaneURL is the default control plane endpoint.
	DefaultControlPlaneURL = "http://localhost:8081"
	// ConfigFileName is the name of the config file.
	ConfigFileName = ".lightning"
	// ConfigFileType is the config file format.
	ConfigFileType = "yaml"
)

// Config holds the CLI configuration.
type Config struct {
	ControlPlaneURL string `mapstructure:"control_plane_url"`
	AuthToken       string `mapstructure:"auth_token"`
	OutputFormat    string `mapstructure:"output_format"`
}

// Load reads configuration from file and environment.
func Load() (*Config, error) {
	viper.SetConfigName(ConfigFileName)
	viper.SetConfigType(ConfigFileType)

	// Search paths: current dir, home dir
	viper.AddConfigPath(".")
	if home, err := os.UserHomeDir(); err == nil {
		viper.AddConfigPath(home)
	}

	// Environment variables
	viper.SetEnvPrefix("LIGHTNING")
	viper.AutomaticEnv()

	// Defaults
	viper.SetDefault("control_plane_url", DefaultControlPlaneURL)
	viper.SetDefault("output_format", "table")

	// Read config file (ignore if not found)
	if err := viper.ReadInConfig(); err != nil {
		if _, ok := err.(viper.ConfigFileNotFoundError); !ok {
			return nil, fmt.Errorf("error reading config: %w", err)
		}
	}

	var cfg Config
	if err := viper.Unmarshal(&cfg); err != nil {
		return nil, fmt.Errorf("error parsing config: %w", err)
	}

	return &cfg, nil
}

// Save writes the configuration to the user's home directory.
func Save(cfg *Config) error {
	home, err := os.UserHomeDir()
	if err != nil {
		return fmt.Errorf("cannot find home directory: %w", err)
	}

	viper.Set("control_plane_url", cfg.ControlPlaneURL)
	viper.Set("auth_token", cfg.AuthToken)
	viper.Set("output_format", cfg.OutputFormat)

	configPath := filepath.Join(home, ConfigFileName+"."+ConfigFileType)
	return viper.WriteConfigAs(configPath)
}

// GetControlPlaneURL returns the configured control plane URL.
func GetControlPlaneURL() string {
	return viper.GetString("control_plane_url")
}

// GetAuthToken returns the configured auth token.
func GetAuthToken() string {
	return viper.GetString("auth_token")
}

// GetOutputFormat returns the configured output format.
func GetOutputFormat() string {
	return viper.GetString("output_format")
}

// SetControlPlaneURL sets the control plane URL.
func SetControlPlaneURL(url string) {
	viper.Set("control_plane_url", url)
}

// SetAuthToken sets the auth token.
func SetAuthToken(token string) {
	viper.Set("auth_token", token)
}

// SetOutputFormat sets the output format.
func SetOutputFormat(format string) {
	viper.Set("output_format", format)
}
