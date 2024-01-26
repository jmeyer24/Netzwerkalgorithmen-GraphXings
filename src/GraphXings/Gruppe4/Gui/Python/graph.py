import networkx as nx
import matplotlib.pyplot as plt
import sys
import os


# Data structure of parsed data
class GraphData:
    player_name = ""
    player_role = ""
    vertices = []
    edges = []
    game_moves = []

    # Parse the content of the file into the data structure
    def parse_lines(self, lines):
        parse_mode = 0
        for line in lines:
            clean_line = line.strip()
            if clean_line == '\\':
                parse_mode += 1
                continue  # Skip the current line
            if parse_mode == 0:
                self.player_name = clean_line
            elif parse_mode == 1:
                self.player_role = clean_line
            elif parse_mode == 2:
                self.vertices.append(clean_line)
            elif parse_mode == 3:
                v1, v2 = clean_line.split(",")
                self.edges.append((v1, v2))
            elif parse_mode == 4:
                # We have the following format for game moves:
                # role, vertex, x, y, player strategy
                role, vertex, x, y, player_strategy = clean_line.split(",")
                self.game_moves.append((role, vertex, x, y, player_strategy))

    def get_game_moves(self):
        return self.game_moves

    # Returns a dictionary of vertices and their positions
    def get_vertices_position(self):
        positions = {}
        for _, vertex, x, y, _ in self.game_moves:
            positions[vertex] = [int(x), int(y)]
        return positions

    def get_vertices_color(self):
        colors = []
        for role, _, _, _, _ in self.game_moves:
            color = get_color_for_role(role)
            colors.append(color)
        return colors

    # Read the file and parse the content
    def read_file(self, filename):
        f = open(filename, "r")
        lines = f.readlines()
        self.parse_lines(lines)
        # self.printData()
        f.close()

    # Print the data structure for debugging
    def print_data(self):
        print("Player Name: " + self.player_name)
        print("Player Role: " + self.player_role)
        print("Vertices: ")
        print(self.vertices)
        print("Edges: ")
        print(self.edges)
        print("Game Moves: ")
        print(self.game_moves)


def get_color_for_role(role):
    return (1, 0, 0, 0.1) if role == "MAX" else (0, 0, 1, 0.1)


def render_gamemove_images(graph_data: GraphData, filename_prefix):
    # Add nodes
    node_positions_todo = graph_data.get_vertices_position()
    edges_todo = graph_data.edges
    node_colors = []
    node_positions = {}
    nodes = []
    edges = []

    game_moves = graph_data.get_game_moves()
    for game_move, (role, vertex, x, y, player_strategy) in enumerate(game_moves):
        nodes.append(vertex)
        node_positions[vertex] = node_positions_todo.pop(vertex)
        node_colors.append(get_color_for_role(role))
        for edge in edges_todo[:]:
            (v1, v2) = edge
            if (vertex == v1 or vertex == v2) and v1 in nodes and v2 in nodes:
                edges_todo.remove(edge)
                edges.append(edge)

        # Create an empty graph
        G = nx.Graph()
        # Add nodes
        G.add_nodes_from(nodes)
        # Add edges
        G.add_edges_from(edges)

        # Open new plot
        plt.figure()

        plt.grid(True)

        # Draw the graph with assigned colors
        # nx.draw_networkx(G, nodelist=nodes, edgelist=edges, pos=node_positions_todo, with_labels=True,
        #                  node_color=node_colors, node_size=200, font_weight='medium')
        nx.draw_networkx(G, pos=node_positions, with_labels=True, node_color=node_colors, node_size=200,
                         font_weight='medium')

        filepath = filename_prefix + f"_{game_move:010d}.png"

        # Display the graph
        plt.savefig(filepath, format="PNG")
        plt.close()


# Draw the graph
def show_graph_window(graph_data: GraphData):
    # Create an empty graph
    G = nx.Graph()

    # Add nodes
    node_colors = graph_data.get_vertices_color()
    node_positions = graph_data.get_vertices_position()
    G.add_nodes_from(node_positions)

    # Add edges
    G.add_edges_from(graph_data.edges)

    # Assign colors to nodes based on even/odd
    # node_colors = ['blue' if node % 2 == 0 else 'green' for node in G.nodes()]
    # plt.figure("Game Graph", figsize=(1000, 1000), dpi=1)

    plt.grid(True)

    # Draw the graph with assigned colors
    nx.draw_networkx(G, pos=node_positions, with_labels=True, node_color=node_colors, node_size=200,
                     font_weight='medium')

    # Display the graph
    plt.show()


# Main call
def main(argv):
    if len(argv) > 1:
        # Access the command-line arguments using sys.argv
        filepath = argv[1]
        gd = GraphData()
        gd.read_file(filepath)

        directory = os.path.dirname(filepath)
        basename = os.path.basename(filepath)
        filename, file_extension = os.path.splitext(basename)
        render_gamemove_images(gd, os.path.join(directory, filename))

        # show_graph_window(gd)
    else:
        print("No file-path provided.")


main(sys.argv)
